package io.github.kestalf.futurebackport.entity.sulfur;

import io.github.kestalf.futurebackport.registry.ModAttachments;
import io.github.kestalf.futurebackport.registry.ModItems;
import io.github.kestalf.futurebackport.registry.ModKeys;
import io.github.kestalf.futurebackport.registry.ModParticles;
import io.github.kestalf.futurebackport.registry.ModSounds;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * Sulfur cube (26.2): a passive cube mob that jumps around like a slime. A large cube can absorb a block (fed by hand
 * or picked up from the ground); it then stops moving and behaves like a ball whose physics depend on the block's
 * {@link SulfurCubeArchetype}: hits and most other damage launch it instead of hurting it. Large cubes split into two
 * small ones on death; small ones grow up after 20 minutes (faster when fed slimeballs, never with a golden
 * dandelion). Large cubes can be picked up with a bucket.
 */
public class SulfurCube extends PathfinderMob implements Shearable {
    public static final int SMALL = 1;
    public static final int LARGE = 2;
    private static final int GROW_UP_TICKS = 24000;
    /** Caps on how fast a hit can send a cube, and the smallest bounce that is not damped to rest. */
    private static final double MAX_LAUNCH_SPEED = 1.0;
    private static final double MIN_BOUNCE = 0.15;
    private static final EntityDataAccessor<Integer> SIZE = SynchedEntityData.defineId(SulfurCube.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(SulfurCube.class, EntityDataSerializers.INT);

    public float targetSquish;
    public float squish;
    public float oSquish;
    private boolean wasOnGround;
    /** Growth of a small cube: counts up from -24000; at 0 it becomes large. */
    private int age;
    private int pickupTimer;
    private boolean fromBucket;
    private int contactCooldown;
    private int pushSoundCooldown;
    private boolean exploded;

    public SulfurCube(EntityType<? extends SulfurCube> type, Level level) {
        super(type, level);
        this.moveControl = new CubeMoveControl(this);
        setDropChance(EquipmentSlot.BODY, 2.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.4);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new FollowGoal(this));
        goalSelector.addGoal(3, new RandomDirectionGoal(this));
        goalSelector.addGoal(5, new KeepOnJumpingGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SIZE, LARGE);
        builder.define(FUSE, -1);
    }

    // ------------------------------------------------------------------ size and growth

    public int getSize() {
        return entityData.get(SIZE);
    }

    public boolean isSmall() {
        return getSize() == SMALL;
    }

    public void setSize(int size) {
        entityData.set(SIZE, size);
        reapplyPosition();
        refreshDimensions();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(size == LARGE ? 8.0 : 4.0);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2 + 0.1 * size);
        setHealth(getMaxHealth());
        age = size == SMALL ? -GROW_UP_TICKS : 0;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SIZE.equals(key)) {
            refreshDimensions();
            setYRot(yHeadRot);
            yBodyRot = yHeadRot;
            if (isInWater() && random.nextInt(20) == 0) doWaterSplashEffect();
        }
        super.onSyncedDataUpdated(key);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(getSize());
    }

    @Override
    public int getBaseExperienceReward() {
        return isSmall() ? 0 : 1 + random.nextInt(2);
    }

    private boolean isAgeLocked() {
        return getData(ModAttachments.AGE_LOCKED);
    }

    /** Slimeballs speed growth up by a tenth of the remaining time. */
    private void feed(Player player, ItemStack stack) {
        if (!level().isClientSide) {
            age = Math.min(0, age + (-age / 10));
            stack.consume(1, player);
            playSound(ModSounds.event("entity.small_sulfur_cube.eat").get(), 1.0F, getVoicePitch());
            ((ServerLevel) level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, getX(), getY(0.5), getZ(), 5, 0.25, 0.25, 0.25, 0.0);
        }
    }

    // ------------------------------------------------------------------ absorbed block

    public ItemStack getBlock() {
        return getItemBySlot(EquipmentSlot.BODY);
    }

    public boolean hasBlock() {
        return !getBlock().isEmpty();
    }

    @Nullable
    public SulfurCubeArchetype getArchetype() {
        return SulfurCubeArchetype.of(getBlock());
    }

    public static boolean canAbsorb(ItemStack stack) {
        return SulfurCubeArchetype.of(stack) != null;
    }

    /** Absorbs one item of the stack, dropping the block absorbed before. */
    public void absorb(ItemStack stack) {
        ejectBlock(false);
        setItemSlot(EquipmentSlot.BODY, stack.copyWithCount(1));
        setDropChance(EquipmentSlot.BODY, 2.0F);
        getNavigation().stop();
        setZza(0.0F);
        playSound(ModSounds.event("entity.sulfur_cube.absorb").get(), 1.0F, 1.0F);
        gameEvent(GameEvent.EQUIP);
    }

    private void ejectBlock(boolean sound) {
        if (!hasBlock()) return;
        ItemStack block = getBlock();
        setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
        spawnAtLocation(block, getBbHeight() / 2);
        if (sound) playSound(ModSounds.event("entity.sulfur_cube.eject").get(), 1.0F, 1.0F);
    }

    @Override
    public void shear(SoundSource source) {
        ejectBlock(true);
        pickupTimer = 100;
    }

    @Override
    public boolean readyForShearing() {
        return isAlive() && hasBlock() && !isIgnited();
    }

    @Override
    public boolean canBeLeashed() {
        return hasBlock() && super.canBeLeashed();
    }

    // ------------------------------------------------------------------ explosive cubes

    public int getFuse() {
        return entityData.get(FUSE);
    }

    public boolean isIgnited() {
        return getFuse() >= 0;
    }

    public void ignite(int fuse) {
        if (isIgnited() || getArchetype() != SulfurCubeArchetype.EXPLOSIVE) return;
        entityData.set(FUSE, fuse);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.TNT_PRIMED, SoundSource.HOSTILE, 1.0F, 1.0F);
        gameEvent(GameEvent.PRIME_FUSE);
    }

    private void explode() {
        exploded = true;
        setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
        level().explode(this, getX(), getY(0.0625), getZ(), SulfurCubeArchetype.EXPLOSION_POWER, false, Level.ExplosionInteraction.TNT);
        dead = true;
        triggerOnDeathMobEffects(RemovalReason.KILLED);
        discard();
    }

    // ------------------------------------------------------------------ interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isSmall()) {
            if (stack.is(ModTags.Items.SULFUR_CUBE_FOOD) && !isAgeLocked()) {
                feed(player, stack);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            return super.mobInteract(player, hand);
        }
        if (getArchetype() == SulfurCubeArchetype.EXPLOSIVE && !isIgnited() && (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
            if (!level().isClientSide) {
                ignite(SulfurCubeArchetype.FUSE);
                if (stack.is(Items.FLINT_AND_STEEL)) {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                } else {
                    stack.consume(1, player);
                    level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isIgnited()) return super.mobInteract(player, hand);
        if (stack.is(Items.BUCKET) && isAlive()) {
            playSound(ModSounds.event("item.bucket.fill_sulfur_cube").get(), 1.0F, 1.0F);
            if (!level().isClientSide) {
                ItemStack bucket = io.github.kestalf.futurebackport.item.SulfurCubeBucketItem.fill(this);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, bucket, false));
                discard();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (canAbsorb(stack)) {
            if (!level().isClientSide) {
                absorb(stack);
                stack.consume(1, player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    public boolean isFromBucket() {
        return fromBucket;
    }

    public void setFromBucket(boolean fromBucket) {
        this.fromBucket = fromBucket;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || fromBucket;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !fromBucket && !hasCustomName() && !hasBlock();
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force) {
        return (force || !(vehicle instanceof Boat)) && super.startRiding(vehicle, force);
    }

    // ------------------------------------------------------------------ damage and launching

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        boolean explosive = getArchetype() == SulfurCubeArchetype.EXPLOSIVE;
        if (explosive && !level().isClientSide) {
            if (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || attackerHasFireAspect(source)) {
                ignite(SulfurCubeArchetype.FUSE);
            } else if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION) && source.getDirectEntity() != this) {
                ignite(15 + random.nextInt(30));
            }
        }
        if (isIgnited()) return false;
        if (hasBlock() && source.is(ModTags.DamageTypes.SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO)) {
            if (!level().isClientSide && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) launch(source, amount);
            return false;
        }
        return super.hurt(source, amount);
    }

    private boolean attackerHasFireAspect(DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity attacker)) return false;
        var fireAspect = registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FIRE_ASPECT);
        return attacker.getMainHandItem().getEnchantmentLevel(fireAspect) > 0;
    }

    /** Launches the cube away from where it was hit, harder for stronger hits. */
    private void launch(DamageSource source, float amount) {
        SulfurCubeArchetype archetype = getArchetype();
        if (archetype == null) return;
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        Vec3 centre = getBoundingBox().getCenter();
        Vec3 direction;
        float bonus = 0.0F;
        if (direct instanceof Projectile projectile) {
            direction = projectile.getDeltaMovement();
            if (projectile instanceof AbstractArrow arrow && arrow.getWeaponItem() != null && level() instanceof ServerLevel server) {
                bonus = EnchantmentHelper.modifyKnockback(server, arrow.getWeaponItem(), this, source, 0.0F);
            }
        } else if (direct != null) {
            // Opposite of where the hit lands on the body: hitting the bottom sends it up, the side sends it away.
            Vec3 eye = direct.getEyePosition();
            Optional<Vec3> hit = getBoundingBox().clip(eye, eye.add(direct.getLookAngle().scale(8.0)));
            direction = centre.subtract(hit.orElse(direct.position()));
            if (direct instanceof LivingEntity living && level() instanceof ServerLevel server) {
                bonus = EnchantmentHelper.modifyKnockback(server, living.getWeaponItem(), this, source,
                        (float) living.getAttributeValue(Attributes.ATTACK_KNOCKBACK));
                if (living.isSprinting()) bonus += 1.0F;
            }
        } else if (source.getSourcePosition() != null) {
            direction = centre.subtract(source.getSourcePosition());
        } else {
            return;
        }
        if (direction.lengthSqr() < 1.0E-6) return;
        float power = (0.5F + 0.5F * Mth.sqrt(Math.max(amount, 1.0F))) * (1.0F - archetype.knockbackResistance);
        Vec3 horizontal = new Vec3(direction.x, 0.0, direction.z);
        horizontal = horizontal.lengthSqr() < 1.0E-6 ? Vec3.ZERO : horizontal.normalize();
        // Only a hit clearly from below (more up than sideways) adds lift beyond the archetype's own.
        double lift = Math.max(0.0, direction.normalize().y - 0.5) * 2.0;
        double up = archetype.verticalPower * power * (1.0 + lift * 2.0);
        if (!onGround()) up *= 1.5;
        double speed = Math.min(archetype.horizontalPower * power + 0.5 * bonus, MAX_LAUNCH_SPEED);
        Vec3 push = horizontal.scale(speed * (1.0 - lift * 0.5)).add(0.0, Math.min(up, MAX_LAUNCH_SPEED), 0.0);
        setDeltaMovement(getDeltaMovement().scale(0.5).add(push));
        hasImpulse = true;
        hurtMarked = true;
        if (attacker instanceof LivingEntity living) setLastHurtByMob(living);
        playSound(archetype.hitSound(), 1.0F, 1.0F);
        gameEvent(GameEvent.ENTITY_DAMAGE, attacker);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return !hasBlock() && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public void push(double x, double y, double z) {
        SulfurCubeArchetype archetype = getArchetype();
        if (archetype == null) {
            super.push(x, y, z);
            return;
        }
        double factor = Math.max(0.0, 1.0 - archetype.knockbackResistance) * 0.5;
        super.push(x * factor, y * factor, z * factor);
        if (!level().isClientSide && pushSoundCooldown <= 0 && getDeltaMovement().horizontalDistance() > archetype.pushSoundThreshold) {
            playSound(archetype.pushSound(), 0.6F, 1.0F);
            pushSoundCooldown = (int) (archetype.pushSoundCooldown * 20);
        }
    }

    // ------------------------------------------------------------------ ticking and physics

    @Override
    public void tick() {
        oSquish = squish;
        squish += (targetSquish - squish) * 0.5F;
        super.tick();
        if (!hasBlock()) {
            if (onGround() && !wasOnGround) {
                float radius = getBbWidth() / 2.0F;
                for (int i = 0; i < getSize() * 8; i++) {
                    float angle = random.nextFloat() * Mth.TWO_PI;
                    float distance = random.nextFloat() * 0.5F + 0.5F;
                    level().addParticle(ModParticles.SULFUR_CUBE_GOO, getX() + Mth.sin(angle) * radius * distance, getY(),
                            getZ() + Mth.cos(angle) * radius * distance, 0.0, 0.0, 0.0);
                }
                playSound(squishSound(), getSoundVolume(), getVoicePitch());
                targetSquish = -0.5F;
            } else if (!onGround() && wasOnGround) {
                targetSquish = 1.0F;
            }
        } else {
            targetSquish = 0.0F;
            if (onGround() && !wasOnGround && getDeltaMovement().y > 0.1) {
                playSound(ModSounds.event("entity.sulfur_cube.bounce").get(), 0.5F, getVoicePitch());
            }
        }
        wasOnGround = onGround();
        targetSquish *= 0.6F;

        if (level().isClientSide) return;
        if (pickupTimer > 0) pickupTimer--;
        if (pushSoundCooldown > 0) pushSoundCooldown--;
        if (isSmall() && !isAgeLocked() && ++age >= 0) {
            setSize(LARGE);
            setHealth(getMaxHealth());
        }
        SulfurCubeArchetype archetype = getArchetype();
        if (archetype == SulfurCubeArchetype.EXPLOSIVE) {
            if (!isIgnited() && level().hasNeighborSignal(BlockPos.containing(getBoundingBox().getCenter()))) ignite(SulfurCubeArchetype.FUSE);
            if (isIgnited()) {
                int fuse = getFuse() - 1;
                entityData.set(FUSE, fuse);
                if (fuse <= 0) explode();
            }
        } else if (isIgnited()) {
            entityData.set(FUSE, -1);
        }
        if (archetype == SulfurCubeArchetype.HOT && --contactCooldown <= 0) {
            contactCooldown = 10;
            burnTouching();
        }
        if (!hasBlock() && !isSmall() && pickupTimer == 0 && isAlive() && EventHooks.canEntityGrief(level(), this)) {
            for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(0.5))) {
                if (!item.isRemoved() && !item.hasPickUpDelay() && canAbsorb(item.getItem())) {
                    absorb(item.getItem());
                    take(item, 1);
                    item.getItem().shrink(1);
                    if (item.getItem().isEmpty()) item.discard();
                    break;
                }
            }
        }
    }

    private void burnTouching() {
        DamageSource source = new DamageSource(registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ModKeys.SULFUR_CUBE_HOT));
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.05), e -> e != this && e.isAlive())) {
            if (!entity.isSteppingCarefully()) entity.hurt(source, SulfurCubeArchetype.CONTACT_DAMAGE);
        }
    }

    @Override
    public void travel(Vec3 input) {
        SulfurCubeArchetype archetype = getArchetype();
        if (archetype == null || !isControlledByLocalInstance()) {
            super.travel(input);
            return;
        }
        // A cube with a block does not move by itself; it rolls, slides and bounces like a ball.
        Vec3 velocity = getDeltaMovement();
        double gravity = getGravity();
        if (isInWater() || isInLava()) {
            double drag = isInWater() ? 0.8 : 0.5;
            velocity = velocity.multiply(drag, drag, drag);
            boolean floats = archetype.buoyant && getFluidHeight(isInWater() ? FluidTags.WATER : FluidTags.LAVA) > getBbHeight() * 0.4;
            velocity = velocity.add(0.0, floats ? 0.04 : -gravity / 4.0, 0.0);
        } else {
            velocity = velocity.add(0.0, -gravity, 0.0);
        }
        Vec3 before = position();
        move(net.minecraft.world.entity.MoverType.SELF, velocity);
        Vec3 moved = position().subtract(before);
        double x = velocity.x, y = velocity.y, z = velocity.z;
        if (horizontalCollision) {
            if (Math.abs(moved.x - velocity.x) > 1.0E-4) x = -velocity.x * archetype.bounciness;
            if (Math.abs(moved.z - velocity.z) > 1.0E-4) z = -velocity.z * archetype.bounciness;
        }
        if (verticalCollision) {
            y = -velocity.y * archetype.bounciness;
            if (velocity.y < 0.0) {
                // The impact also brakes the cube sideways, more on grippy blocks and on hard landings.
                double horizontal = Math.sqrt(x * x + z * z);
                if (horizontal > 1.0E-4) {
                    double braked = Math.max(0.0, horizontal - archetype.friction * (1.0 + archetype.bounciness) * -velocity.y * 2.0);
                    x *= braked / horizontal;
                    z *= braked / horizontal;
                }
                if (y < MIN_BOUNCE) y = 0.0;
            } else {
                y = Math.min(y, 0.0);
            }
        }
        if (onGround()) {
            BlockPos below = getBlockPosBelowThatAffectsMyMovement();
            double friction = level().getBlockState(below).getFriction(level(), below, this) * 0.91;
            double keep = Mth.clamp(1.0 - (1.0 - friction) * archetype.friction, 0.0, 1.0);
            x *= keep;
            z *= keep;
        } else if (!isInWater() && !isInLava()) {
            double keep = 1.0 - 0.09 * archetype.airDrag;
            x *= keep;
            z *= keep;
            y *= 1.0 - 0.02 * archetype.airDrag;
        }
        setDeltaMovement(x, y, z);
        calculateEntityAnimation(false);
    }

    @Override
    public void jumpFromGround() {
        Vec3 velocity = getDeltaMovement();
        setDeltaMovement(velocity.x, getJumpPower(), velocity.z);
        hasImpulse = true;
        net.neoforged.neoforge.common.CommonHooks.onLivingJump(this);
    }

    int getJumpDelay() {
        return random.nextInt(20) + 10;
    }

    // ------------------------------------------------------------------ splitting, saving, sounds

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason == RemovalReason.KILLED && isDeadOrDying() && !isSmall() && !exploded) {
            float offset = getBbWidth() / 4.0F;
            for (int i = 0; i < 2; i++) {
                SulfurCube small = (SulfurCube) getType().create(level());
                if (small == null) continue;
                if (isPersistenceRequired()) small.setPersistenceRequired();
                small.setCustomName(getCustomName());
                small.setNoAi(isNoAi());
                small.setInvulnerable(isInvulnerable());
                small.setSize(SMALL);
                float dx = (i - 0.5F) * offset * 2.0F;
                small.moveTo(getX() + dx, getY() + 0.5, getZ() + (random.nextFloat() - 0.5F) * offset, random.nextFloat() * 360.0F, 0.0F);
                level().addFreshEntity(small);
            }
        }
        super.remove(reason);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData data) {
        setSize(LARGE);
        return super.finalizeSpawn(level, difficulty, spawnType, data);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Size", getSize() - 1);
        tag.putInt("Age", age);
        tag.putInt("pickup_timer", pickupTimer);
        tag.putBoolean("from_bucket", fromBucket);
        tag.putInt("fuse", getFuse());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int size = Mth.clamp(tag.getInt("Size") + 1, SMALL, LARGE);
        entityData.set(SIZE, size);
        refreshDimensions();
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2 + 0.1 * size);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(size == LARGE ? 8.0 : 4.0);
        age = size == SMALL ? tag.getInt("Age") : 0;
        pickupTimer = tag.getInt("pickup_timer");
        fromBucket = tag.getBoolean("from_bucket");
        entityData.set(FUSE, tag.contains("fuse") ? tag.getInt("fuse") : -1);
    }

    @Override
    public void refreshDimensions() {
        double x = getX(), y = getY(), z = getZ();
        super.refreshDimensions();
        setPos(x, y, z);
    }

    @Override
    protected float getSoundVolume() {
        return isSmall() ? 0.6F : 1.0F;
    }

    private String prefix() {
        return isSmall() ? "entity.small_sulfur_cube." : "entity.sulfur_cube.";
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.event(prefix() + "hurt").get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.event(prefix() + "death").get();
    }

    SoundEvent squishSound() {
        return ModSounds.event(prefix() + "squish").get();
    }

    SoundEvent jumpSound() {
        return ModSounds.event(prefix() + "jump").get();
    }

    @Override
    public int getMaxHeadXRot() {
        return 0;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.SULFUR_CUBE_SPAWN_EGG.get());
    }

    // ------------------------------------------------------------------ AI (slime-like hopping)

    /** Moves by hopping in the facing direction, like a slime. */
    static class CubeMoveControl extends MoveControl {
        private final SulfurCube cube;
        private float yRot;
        private int jumpDelay;
        private boolean eager;

        CubeMoveControl(SulfurCube cube) {
            super(cube);
            this.cube = cube;
            this.yRot = 180.0F * cube.getYRot() / Mth.PI;
        }

        void setDirection(float yRot, boolean eager) {
            this.yRot = yRot;
            this.eager = eager;
        }

        void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            mob.setYRot(rotlerp(mob.getYRot(), yRot, 90.0F));
            mob.yHeadRot = mob.getYRot();
            mob.yBodyRot = mob.getYRot();
            if (operation != Operation.MOVE_TO || cube.hasBlock()) {
                mob.setZza(0.0F);
                return;
            }
            operation = Operation.WAIT;
            if (mob.onGround()) {
                mob.setSpeed((float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                if (jumpDelay-- <= 0) {
                    jumpDelay = cube.getJumpDelay();
                    if (eager) jumpDelay /= 3;
                    cube.getJumpControl().jump();
                    cube.playSound(cube.jumpSound(), cube.getSoundVolume(), cube.getVoicePitch());
                } else {
                    cube.xxa = 0.0F;
                    cube.zza = 0.0F;
                    mob.setSpeed(0.0F);
                }
            } else {
                mob.setSpeed((float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            }
        }
    }

    private abstract static class CubeGoal extends Goal {
        protected final SulfurCube cube;

        CubeGoal(SulfurCube cube, EnumSet<Flag> flags) {
            this.cube = cube;
            setFlags(flags);
        }

        protected boolean free() {
            return !cube.hasBlock() && !cube.isIgnited() && cube.getMoveControl() instanceof CubeMoveControl;
        }

        protected CubeMoveControl control() {
            return (CubeMoveControl) cube.getMoveControl();
        }
    }

    static class FloatGoal extends CubeGoal {
        FloatGoal(SulfurCube cube) {
            super(cube, EnumSet.of(Flag.JUMP, Flag.MOVE));
            cube.getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return free() && (cube.isInWater() || cube.isInLava());
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (cube.getRandom().nextFloat() < 0.8F) cube.getJumpControl().jump();
            control().setWantedMovement(1.2);
        }
    }

    /** Small cubes follow players holding slimeballs; large ones go for absorbable blocks, held or on the ground. */
    static class FollowGoal extends CubeGoal {
        @Nullable
        private Entity target;
        private int timeout;

        FollowGoal(SulfurCube cube) {
            super(cube, EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!free()) return false;
            target = findTarget();
            return target != null;
        }

        @Nullable
        private Entity findTarget() {
            Level level = cube.level();
            Player player = level.getNearestPlayer(cube.getX(), cube.getY(), cube.getZ(), 10.0, p -> p instanceof Player candidate && !candidate.isSpectator() && wants(candidate));
            if (player != null) return player;
            if (cube.isSmall() || cube.pickupTimer > 0 || !EventHooks.canEntityGrief(level, cube)) return null;
            List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, cube.getBoundingBox().inflate(8.0, 4.0, 8.0),
                    item -> canAbsorb(item.getItem()));
            return items.stream().min(java.util.Comparator.comparingDouble(cube::distanceToSqr)).orElse(null);
        }

        private boolean wants(Player player) {
            for (InteractionHand hand : InteractionHand.values()) {
                ItemStack stack = player.getItemInHand(hand);
                if (cube.isSmall() ? stack.is(ModTags.Items.SULFUR_CUBE_FOOD) : canAbsorb(stack)) return true;
            }
            return false;
        }

        @Override
        public void start() {
            timeout = reducedTickDelay(300);
        }

        @Override
        public boolean canContinueToUse() {
            return free() && target != null && target.isAlive() && --timeout > 0 && cube.distanceToSqr(target) < 144.0
                    && (!(target instanceof Player player) || wants(player));
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (target == null) return;
            cube.lookAt(target, 10.0F, 10.0F);
            if (cube.distanceToSqr(target) > 2.0) control().setDirection(cube.getYRot(), true);
        }
    }

    static class RandomDirectionGoal extends CubeGoal {
        private float chosenDegrees;
        private int nextRandomizeTime;

        RandomDirectionGoal(SulfurCube cube) {
            super(cube, EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return free() && (cube.onGround() || cube.isInWater() || cube.isInLava() || cube.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION));
        }

        @Override
        public void tick() {
            if (--nextRandomizeTime <= 0) {
                nextRandomizeTime = adjustedTickDelay(40 + cube.getRandom().nextInt(60));
                chosenDegrees = cube.getRandom().nextInt(360);
            }
            control().setDirection(chosenDegrees, false);
        }
    }

    static class KeepOnJumpingGoal extends CubeGoal {
        KeepOnJumpingGoal(SulfurCube cube) {
            super(cube, EnumSet.of(Flag.JUMP, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return free() && !cube.isPassenger();
        }

        @Override
        public void tick() {
            control().setWantedMovement(1.0);
        }
    }
}
