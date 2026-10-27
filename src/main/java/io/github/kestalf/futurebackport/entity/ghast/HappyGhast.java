package io.github.kestalf.futurebackport.entity.ghast;

import io.github.kestalf.futurebackport.item.HarnessItem;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModSounds;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Happy ghast (1.21.6): a friendly flying mob grown from a dried ghast. Babies are called ghastlings. Adults can wear a
 * harness, carry up to four players (the first one steers) and lift leashed mobs and boats.
 */
public class HappyGhast extends Animal implements FlyingAnimal {
    public static final int MAX_PASSENGERS = 4;
    private static final float RIDDEN_SPEED = 0.18F;
    private static final int WANDER_RADIUS = 64;
    private static final int HARNESSED_WANDER_RADIUS = 32;

    /** Where the ghast roams around; updated when its harness, rider, leash or tempting player changes. */
    private BlockPos home;
    private boolean wasTempted;
    private boolean wasRidden;

    public HappyGhast(EntityType<? extends HappyGhast> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.FLYING_SPEED, 0.05)
                .add(Attributes.MOVEMENT_SPEED, 0.05)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.1, this::isTemptingItem, false));
        this.goalSelector.addGoal(3, new GhastlingFollowGoal(this));
        this.goalSelector.addGoal(4, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    private boolean isTemptingItem(ItemStack stack) {
        if (stack.is(ModTags.Items.HAPPY_GHAST_FOOD)) return true;
        return stack.is(ModTags.Items.HARNESSES) && !isBaby() && !isHarnessed();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    // ------------------------------------------------------------------ state

    public boolean isHarnessed() {
        return getBodyArmorItem().getItem() instanceof HarnessItem;
    }

    @Override
    public boolean isBodyArmorItem(ItemStack stack) {
        return stack.getItem() instanceof HarnessItem;
    }

    /** Goggles are lowered while a player steers. */
    public boolean areGogglesDown() {
        return getControllingPassenger() instanceof Player;
    }

    public BlockPos home() {
        return home == null ? blockPosition() : home;
    }

    private void resetHome() {
        home = blockPosition();
        restrictTo(home, isHarnessed() || isBaby() ? HARNESSED_WANDER_RADIUS : WANDER_RADIUS);
    }

    @Override
    public float getAgeScale() {
        return isBaby() ? 0.2375F : 1.0F;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.Items.HAPPY_GHAST_FOOD);
    }

    @Override
    public boolean canBreed() {
        return false;
    }

    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob otherParent) {
        return ModEntities.HAPPY_GHAST.get().create(level);
    }

    @Override
    public boolean isFlying() {
        return true;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return true;
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isBaby()) {
            if (stack.is(ModTags.Items.HAPPY_GHAST_FOOD)) {
                // Each snowball takes 10% off the remaining growing time.
                if (!level().isClientSide) {
                    ageUp(-getAge() / 10 / 20, true);
                    usePlayerItem(player, hand, stack);
                    playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.0F);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return super.mobInteract(player, hand);
        }
        if (stack.getItem() instanceof HarnessItem && !isHarnessed()) {
            if (!level().isClientSide) {
                setBodyArmorItem(stack.copyWithCount(1));
                stack.consume(1, player);
                level().playSound(null, this, ModSounds.event("entity.happy_ghast.equip").get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                resetHome();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(Items.SHEARS)) {
            if (cutLeashes(player)) return InteractionResult.sidedSuccess(level().isClientSide);
            if (isHarnessed() && !isVehicle()) {
                if (!level().isClientSide) {
                    spawnAtLocation(getBodyArmorItem().copy(), getBbHeight() * 0.5F);
                    setBodyArmorItem(ItemStack.EMPTY);
                    stack.hurtAndBreak(1, player, getSlotForHand(hand));
                    level().playSound(null, this, ModSounds.event("entity.happy_ghast.unequip").get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                    resetHome();
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (player.isSecondaryUseActive() && attachLeashedEntities(player)) {
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isHarnessed() && canAddPassenger(player) && !player.isSecondaryUseActive()) {
            if (!level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** Hands every mob or boat the player leads over to the ghast, which then carries it. */
    private boolean attachLeashedEntities(Player player) {
        List<Entity> leashed = level().getEntities(this, getBoundingBox().inflate(16.0),
                e -> e instanceof Leashable leashable && leashable.getLeashHolder() == player);
        if (leashed.isEmpty()) return false;
        if (!level().isClientSide) {
            for (Entity entity : leashed) ((Leashable) entity).setLeashedTo(this, true);
        }
        return true;
    }

    private List<Entity> carriedEntities() {
        return level().getEntities(this, getBoundingBox().inflate(12.0),
                e -> e instanceof Leashable leashable && leashable.getLeashHolder() == this);
    }

    private boolean cutLeashes(Player player) {
        List<Entity> carried = carriedEntities();
        if (carried.isEmpty()) return false;
        if (!level().isClientSide) {
            for (Entity entity : carried) ((Leashable) entity).dropLeash(true, !player.getAbilities().instabuild);
            level().playSound(null, this, SoundEvents.SHEEP_SHEAR, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        return true;
    }

    public boolean isCarrying() {
        return !carriedEntities().isEmpty();
    }

    @Override
    public Vec3 getRopeHoldPosition(float partialTicks) {
        return getPosition(partialTicks).add(0.0, 0.2, 0.0);
    }

    // ------------------------------------------------------------------ riding

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < MAX_PASSENGERS && !isBaby();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return isHarnessed() && getFirstPassenger() instanceof Player player ? player : super.getControllingPassenger();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dimensions, float partialTick) {
        int seat = getPassengers().indexOf(passenger);
        double half = dimensions.width() * 0.5 * 0.55;
        Vec3 offset = switch (seat) {
            case 0 -> new Vec3(0.0, 0.0, half);
            case 1 -> new Vec3(-half, 0.0, 0.0);
            case 2 -> new Vec3(0.0, 0.0, -half);
            default -> new Vec3(half, 0.0, 0.0);
        };
        return offset.add(0.0, dimensions.height(), 0.0).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return new Vec3(getX(), getBoundingBox().maxY, getZ());
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if (!level().isClientSide && passenger instanceof Player && getPassengers().size() == 1) {
            level().playSound(null, this, ModSounds.event("entity.happy_ghast.harness_goggles_down").get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide) {
            if (!isVehicle()) {
                level().playSound(null, this, ModSounds.event("entity.happy_ghast.harness_goggles_up").get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            resetHome();
        }
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        setYRot(player.getYRot());
        yRotO = getYRot();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        setXRot(0.0F);
    }

    /** Forward/back follow the rider's view (including up and down), strafing stays horizontal, jumping rises. */
    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        Vec3 look = player.getLookAngle();
        Vec3 direction = Vec3.ZERO;
        if (player.zza != 0.0F) direction = direction.add(look.scale(Math.signum(player.zza)));
        if (player.xxa != 0.0F) {
            Vec3 side = new Vec3(look.z, 0.0, -look.x).normalize();
            direction = direction.add(side.scale(Math.signum(player.xxa)));
        }
        if (player.jumping) direction = direction.add(0.0, 1.0, 0.0);
        return direction.lengthSqr() > 1.0 ? direction.normalize() : direction;
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return RIDDEN_SPEED;
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (getControllingPassenger() instanceof Player && isControlledByLocalInstance()) {
            Vec3 target = travelVector.scale(RIDDEN_SPEED);
            setDeltaMovement(getDeltaMovement().add(target.subtract(getDeltaMovement()).scale(0.1)));
            move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());
            calculateEntityAnimation(true);
            return;
        }
        if (isStandingStill()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    // ------------------------------------------------------------------ standing on top

    /** Players can stand on a happy ghast like on a block; it then holds still. */
    @Override
    public boolean canBeCollidedWith() {
        return !isBaby() && !(getControllingPassenger() instanceof Player);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public boolean isStandingStill() {
        if (isBaby() || isVehicle()) return false;
        AABB box = getBoundingBox();
        AABB top = new AABB(box.minX, box.maxY - 0.1, box.minZ, box.maxX, box.maxY + 0.6, box.maxZ);
        return !level().getEntitiesOfClass(Player.class, top, p -> !p.isSpectator() && p.getVehicle() == null).isEmpty();
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || isStandingStill();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) return;
        if (isStandingStill()) {
            getNavigation().stop();
            setYRot(Direction.fromYRot(getYRot()).toYRot());
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
        boolean tempted = level().getNearestPlayer(this, 10.0) instanceof Player p
                && (isTemptingItem(p.getMainHandItem()) || isTemptingItem(p.getOffhandItem()));
        if (wasTempted && !tempted || wasRidden && !isVehicle()) resetHome();
        wasTempted = tempted;
        wasRidden = isVehicle();
        heal();
        carryLeashed();
    }

    /** One heart every 30 seconds, or every second in rain, snow or at cloud height. */
    private void heal() {
        if (getHealth() >= getMaxHealth()) return;
        boolean fast = !isInWater() && (isInRainOrSnow() || level().dimension() == Level.OVERWORLD && getY() >= 187 && getY() <= 196);
        if (tickCount % (fast ? 20 : 600) == 0) heal(1.0F);
    }

    private boolean isInRainOrSnow() {
        BlockPos pos = BlockPos.containing(getX(), getBoundingBox().maxY, getZ());
        return level().isRaining() && level().getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY();
    }

    /** Leashed entities hang below the ghast instead of being dragged around by the leash. */
    private void carryLeashed() {
        for (Entity entity : carriedEntities()) {
            Vec3 target = new Vec3(getX(), getY() - entity.getBbHeight() - 1.0, getZ());
            Vec3 pull = target.subtract(entity.position());
            if (pull.length() > 1.0) {
                entity.setDeltaMovement(entity.getDeltaMovement().scale(0.6).add(pull.scale(0.15)));
                entity.hurtMarked = true;
            }
            entity.resetFallDistance();
        }
    }

    // ------------------------------------------------------------------ sounds and saving

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.event(isBaby() ? "entity.ghastling.ambient" : "entity.happy_ghast.ambient").get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.event(isBaby() ? "entity.ghastling.hurt" : "entity.happy_ghast.hurt").get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.event(isBaby() ? "entity.ghastling.death" : "entity.happy_ghast.death").get();
    }

    @Override
    protected float getSoundVolume() {
        return isBaby() ? 1.0F : 4.0F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected void ageBoundaryReached() {
        super.ageBoundaryReached();
        if (!isBaby()) resetHome();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("home_pos", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        Optional<BlockPos> saved = NbtUtils.readBlockPos(tag, "home_pos");
        saved.ifPresent(pos -> {
            home = pos;
            restrictTo(pos, isHarnessed() || isBaby() ? HARNESSED_WANDER_RADIUS : WANDER_RADIUS);
        });
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (home == null) resetHome();
    }

    /** Ghastling spawn effects, used when it hatches from a dried ghast. */
    public void playHatchEffects() {
        level().playSound(null, this, ModSounds.event("entity.ghastling.spawn").get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(0.5), getZ(), 12, 0.4, 0.4, 0.4, 0.0);
        }
    }
}
