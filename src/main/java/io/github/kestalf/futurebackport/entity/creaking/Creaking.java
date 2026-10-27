package io.github.kestalf.futurebackport.entity.creaking;

import io.github.kestalf.futurebackport.block.CreakingHeartBlock;
import io.github.kestalf.futurebackport.block.CreakingHeartState;
import io.github.kestalf.futurebackport.block.entity.CreakingHeartBlockEntity;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The creaking: freezes while a survival player looks at it, hunts players otherwise. Creakings spawned by a creaking
 * heart are bound to it: they cannot be hurt, crumble when the heart is broken and vanish during the day.
 */
public class Creaking extends Monster {
    private static final EntityDataAccessor<Boolean> CAN_MOVE = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_ACTIVE = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_TEARING_DOWN = SynchedEntityData.defineId(Creaking.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> HOME_POS = SynchedEntityData.defineId(Creaking.class,
            EntityDataSerializers.OPTIONAL_BLOCK_POS);
    public static final byte EVENT_ATTACK = 4;
    public static final byte EVENT_SPAWN_PARTICLES = 60;
    public static final byte EVENT_SWAY = 66;
    private static final int ATTACK_ANIMATION_DURATION = 15;
    private static final int ATTACK_INTERVAL = 40;
    private static final float ACTIVATION_RANGE_SQ = 144.0F;
    private static final double FOLLOW_RANGE = 32.0;
    private static final double SPEED_MULTIPLIER_WHEN_IDLING = 0.3;
    private static final int INVULNERABILITY_ANIMATION_DURATION = 8;
    public static final int TWITCH_DEATH_DURATION = 45;
    private static final int MAX_PLAYER_STUCK_COUNTER = 4;

    public final AnimationState attackAnimationState = new AnimationState();
    public final AnimationState invulnerabilityAnimationState = new AnimationState();
    public final AnimationState deathAnimationState = new AnimationState();
    private int attackAnimationRemainingTicks;
    private int invulnerabilityAnimationRemainingTicks;
    private boolean eyesGlowing;
    private int nextFlickerTime;
    private int playerStuckCounter;

    public Creaking(EntityType<? extends Creaking> type, Level level) {
        super(type, level);
        this.lookControl = new CreakingLookControl();
        this.moveControl = new CreakingMoveControl();
        this.jumpControl = new CreakingJumpControl();
        if (getNavigation() instanceof GroundPathNavigation navigation) navigation.setCanFloat(true);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.4)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, FOLLOW_RANGE)
                .add(Attributes.STEP_HEIGHT, 1.0625);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this) {
            @Override
            public boolean canUse() {
                return canMove() && super.canUse();
            }
        });
        goalSelector.addGoal(1, new CreakingAttackGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, SPEED_MULTIPLIER_WHEN_IDLING) {
            @Override
            public boolean canUse() {
                return canMove() && !isActive() && super.canUse();
            }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CAN_MOVE, true);
        builder.define(IS_ACTIVE, false);
        builder.define(IS_TEARING_DOWN, false);
        builder.define(HOME_POS, Optional.empty());
    }

    /** Binds this creaking to the heart that spawned it. */
    public void setTransient(CreakingHeartBlockEntity heart) {
        setHomePos(heart.getBlockPos());
        setPathfindingMalus(PathType.DAMAGE_OTHER, 8.0F);
        setPathfindingMalus(PathType.POWDER_SNOW, 8.0F);
        setPathfindingMalus(PathType.LAVA, 8.0F);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0F);
        setPathfindingMalus(PathType.DANGER_FIRE, 0.0F);
    }

    public boolean isHeartBound() {
        return getHomePos() != null;
    }

    public void setHomePos(BlockPos pos) {
        entityData.set(HOME_POS, Optional.of(pos));
    }

    @Nullable
    public BlockPos getHomePos() {
        return entityData.get(HOME_POS).orElse(null);
    }

    public void setTearingDown() {
        entityData.set(IS_TEARING_DOWN, true);
    }

    public boolean isTearingDown() {
        return entityData.get(IS_TEARING_DOWN);
    }

    public boolean canMove() {
        return entityData.get(CAN_MOVE);
    }

    public boolean isActive() {
        return entityData.get(IS_ACTIVE);
    }

    public boolean hasGlowingEyes() {
        return eyesGlowing;
    }

    // ------------------------------------------------------------------ freezing and activation

    @Override
    public void aiStep() {
        if (invulnerabilityAnimationRemainingTicks > 0) invulnerabilityAnimationRemainingTicks--;
        if (attackAnimationRemainingTicks > 0) attackAnimationRemainingTicks--;
        if (!level().isClientSide) {
            boolean couldMove = canMove();
            boolean canMove = checkCanMove();
            if (canMove != couldMove) {
                gameEvent(GameEvent.ENTITY_ACTION);
                if (canMove) {
                    makeSound(ModSounds.event("entity.creaking.unfreeze").get());
                } else {
                    stopInPlace();
                    makeSound(ModSounds.event("entity.creaking.freeze").get());
                }
            }
            entityData.set(CAN_MOVE, canMove);
        }
        super.aiStep();
    }

    private List<Player> nearestPlayers() {
        return level().players().stream()
                .filter(player -> player.distanceToSqr(this) <= FOLLOW_RANGE * FOLLOW_RANGE)
                .sorted(Comparator.comparingDouble(player -> player.distanceToSqr(this)))
                .map(player -> (Player) player)
                .toList();
    }

    private boolean checkCanMove() {
        List<Player> players = nearestPlayers();
        boolean active = isActive();
        if (players.isEmpty()) {
            if (active) deactivate();
            return true;
        }
        boolean anyTarget = false;
        for (Player player : players) {
            if (!canAttack(player) || isAlliedTo(player)) continue;
            anyTarget = true;
            boolean disguised = player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN);
            if ((!active || !disguised) && isLookingAtMe(player)) {
                if (active) return false;
                if (player.distanceToSqr(this) < ACTIVATION_RANGE_SQ) {
                    activate(player);
                    return false;
                }
            }
        }
        if (!anyTarget && active) deactivate();
        return true;
    }

    /** True if the player's view direction is within 60 degrees of the creaking's eyes, middle or feet, unobstructed. */
    private boolean isLookingAtMe(Player player) {
        Vec3 view = player.getViewVector(1.0F).normalize();
        for (double y : new double[]{getEyeY(), getY() + 0.5 * getScale(), (getEyeY() + getY()) / 2.0}) {
            Vec3 toMe = new Vec3(getX() - player.getX(), y - player.getEyeY(), getZ() - player.getZ()).normalize();
            if (view.dot(toMe) > 0.5 && hasVisualLineOfSight(player, y)) return true;
        }
        return false;
    }

    private boolean hasVisualLineOfSight(Player player, double y) {
        Vec3 from = player.getEyePosition();
        Vec3 to = new Vec3(getX(), y, getZ());
        if (to.distanceToSqr(from) > 128.0 * 128.0) return false;
        return level().clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player)).getType() == HitResult.Type.MISS;
    }

    private void activate(Player player) {
        setTarget(player);
        gameEvent(GameEvent.ENTITY_ACTION);
        makeSound(ModSounds.event("entity.creaking.activate").get());
        entityData.set(IS_ACTIVE, true);
    }

    private void deactivate() {
        setTarget(null);
        gameEvent(GameEvent.ENTITY_ACTION);
        makeSound(ModSounds.event("entity.creaking.deactivate").get());
        entityData.set(IS_ACTIVE, false);
    }

    /** Counts heart updates during which a player's eyes are inside this creaking. */
    public boolean playerIsStuckInYou() {
        AABB box = getBoundingBox();
        for (Player player : nearestPlayers()) {
            if (box.contains(player.getEyePosition())) {
                return ++playerStuckCounter > MAX_PLAYER_STUCK_COUNTER;
            }
        }
        playerStuckCounter = 0;
        return false;
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && canMove();
    }

    @Override
    public void push(double x, double y, double z) {
        if (canMove()) super.push(x, y, z);
    }

    // ------------------------------------------------------------------ heart binding, damage and death

    @Override
    public void tick() {
        if (!level().isClientSide) {
            BlockPos home = getHomePos();
            if (home != null && !(level().getBlockEntity(home) instanceof CreakingHeartBlockEntity heart && heart.isProtector(this))) {
                setHealth(0.0F);
            }
        }
        super.tick();
        if (level().isClientSide) {
            attackAnimationState.animateWhen(attackAnimationRemainingTicks > 0, tickCount);
            invulnerabilityAnimationState.animateWhen(invulnerabilityAnimationRemainingTicks > 0, tickCount);
            deathAnimationState.animateWhen(isTearingDown(), tickCount);
            if (isTearingDown()) {
                if (deathTime > nextFlickerTime) {
                    nextFlickerTime = deathTime + random.nextIntBetweenInclusive(eyesGlowing ? 2 : deathTime / 4, eyesGlowing ? 8 : deathTime / 2);
                    eyesGlowing = !eyesGlowing;
                }
            } else {
                eyesGlowing = isActive();
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        BlockPos home = getHomePos();
        if (home == null || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        if (level().isClientSide || isInvulnerableTo(source) || invulnerabilityAnimationRemainingTicks > 0 || isDeadOrDying()) return false;
        Player player = blameSourceForDamage(source);
        Entity direct = source.getDirectEntity();
        if (!(direct instanceof LivingEntity) && !(direct instanceof Projectile) && player == null) return false;
        invulnerabilityAnimationRemainingTicks = INVULNERABILITY_ANIMATION_DURATION;
        level().broadcastEntityEvent(this, EVENT_SWAY);
        gameEvent(GameEvent.ENTITY_ACTION);
        if (level().getBlockEntity(home) instanceof CreakingHeartBlockEntity heart && heart.isProtector(this)) {
            if (player != null) heart.creakingHurt();
            playHurtSound(source);
        }
        return true;
    }

    @Nullable
    private Player blameSourceForDamage(DamageSource source) {
        Entity attacker = source.getEntity();
        Player player = null;
        if (attacker instanceof Player direct) {
            player = direct;
        } else if (attacker instanceof TamableAnimal pet && pet.getOwner() instanceof Player owner) {
            player = owner;
        }
        if (player != null) setLastHurtByPlayer(player);
        return player;
    }

    /** Called when the heart is broken by a player: the kill counts for that player and the creaking twitches. */
    public void creakingDeathEffects(DamageSource source) {
        blameSourceForDamage(source);
        die(source);
        makeSound(ModSounds.event("entity.creaking.twitch").get());
    }

    @Override
    protected void tickDeath() {
        if (isHeartBound() && isTearingDown()) {
            deathTime++;
            if (!level().isClientSide && deathTime > TWITCH_DEATH_DURATION && !isRemoved()) tearDown();
        } else {
            super.tickDeath();
        }
    }

    /** Crumbles into particles and disappears. */
    public void tearDown() {
        if (level() instanceof ServerLevel server) {
            AABB box = getBoundingBox();
            Vec3 center = box.getCenter();
            double dx = box.getXsize() * 0.3, dy = box.getYsize() * 0.3, dz = box.getZsize() * 0.3;
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.PALE_OAK.wood.get().defaultBlockState()),
                    center.x, center.y, center.z, 100, dx, dy, dz, 0.0);
            BlockState heart = ModBlocks.CREAKING_HEART.get().defaultBlockState().setValue(CreakingHeartBlock.STATE, CreakingHeartState.AWAKE);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, heart), center.x, center.y, center.z, 10, dx, dy, dz, 0.0);
        }
        makeSound(getDeathSound());
        remove(RemovalReason.DISCARDED);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SWAY) {
            invulnerabilityAnimationRemainingTicks = INVULNERABILITY_ANIMATION_DURATION;
            playHurtSound(damageSources().generic());
        } else if (id == EVENT_ATTACK) {
            attackAnimationRemainingTicks = ATTACK_ANIMATION_DURATION;
            playAttackSound();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (!(target instanceof LivingEntity)) return false;
        attackAnimationRemainingTicks = ATTACK_ANIMATION_DURATION;
        level().broadcastEntityEvent(this, EVENT_ATTACK);
        return super.doHurtTarget(target);
    }

    @Override
    public boolean fireImmune() {
        return isHeartBound() || super.fireImmune();
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return !isHeartBound() && super.canUsePortal(allowPassengers);
    }

    @Override
    protected void updateWalkAnimation(float distance) {
        walkAnimation.update(Math.min(distance * 25.0F, 3.0F), 0.4F);
    }

    // ------------------------------------------------------------------ sounds

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return isActive() ? null : ModSounds.event("entity.creaking.ambient").get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isHeartBound() ? ModSounds.event("entity.creaking.sway").get() : super.getHurtSound(source);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.event("entity.creaking.death").get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.event("entity.creaking.step").get(), 0.15F, 1.0F);
    }

    @Override
    protected void playAttackSound() {
        makeSound(ModSounds.event("entity.creaking.attack").get());
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        BlockPos home = getHomePos();
        if (home != null) tag.put("home_pos", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "home_pos").ifPresent(this::setHomePos);
    }

    // ------------------------------------------------------------------ movement controls that respect freezing

    @Override
    protected BodyRotationControl createBodyControl() {
        return new BodyRotationControl(this) {
            @Override
            public void clientTick() {
                if (canMove()) super.clientTick();
            }
        };
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new CreakingPathNavigation(level);
    }

    private final class CreakingLookControl extends LookControl {
        CreakingLookControl() {
            super(Creaking.this);
        }

        @Override
        public void tick() {
            if (canMove()) super.tick();
        }
    }

    private final class CreakingMoveControl extends MoveControl {
        CreakingMoveControl() {
            super(Creaking.this);
        }

        @Override
        public void tick() {
            if (canMove()) super.tick();
        }
    }

    private final class CreakingJumpControl extends JumpControl {
        CreakingJumpControl() {
            super(Creaking.this);
        }

        @Override
        public void tick() {
            if (canMove()) {
                super.tick();
            } else {
                setJumping(false);
            }
        }
    }

    private final class CreakingPathNavigation extends GroundPathNavigation {
        CreakingPathNavigation(Level level) {
            super(Creaking.this, level);
        }

        @Override
        public void tick() {
            if (canMove()) super.tick();
        }

        @Override
        protected PathFinder createPathFinder(int maxVisitedNodes) {
            nodeEvaluator = new HomeNodeEvaluator();
            nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(nodeEvaluator, maxVisitedNodes);
        }
    }

    /** Heart-bound creakings never path further than 32 blocks from their heart. */
    private final class HomeNodeEvaluator extends WalkNodeEvaluator {
        @Override
        public PathType getPathType(PathfindingContext context, int x, int y, int z) {
            BlockPos home = getHomePos();
            if (home == null) return super.getPathType(context, x, y, z);
            double distance = home.distSqr(new Vec3i(x, y, z));
            return distance > 1024.0 && distance >= home.distSqr(context.mobPosition()) ? PathType.BLOCKED : super.getPathType(context, x, y, z);
        }
    }

    private final class CreakingAttackGoal extends MeleeAttackGoal {
        private int cooldown;

        CreakingAttackGoal() {
            super(Creaking.this, 1.0, true);
        }

        @Override
        public void tick() {
            if (cooldown > 0) cooldown--;
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (canMove() && cooldown <= 0 && isWithinMeleeAttackRange(target) && getSensing().hasLineOfSight(target)) {
                cooldown = adjustedTickDelay(ATTACK_INTERVAL);
                swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                doHurtTarget(target);
            }
        }
    }
}
