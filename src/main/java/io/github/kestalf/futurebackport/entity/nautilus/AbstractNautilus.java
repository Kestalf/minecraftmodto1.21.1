package io.github.kestalf.futurebackport.entity.nautilus;

import io.github.kestalf.futurebackport.item.NautilusArmorItem;
import io.github.kestalf.futurebackport.registry.ModEffects;
import io.github.kestalf.futurebackport.registry.ModSounds;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.animal.Pufferfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Shared behaviour of the nautilus and the zombie nautilus: an aquatic, tameable mount with a saddle and body armor
 * slot that its rider steers in three dimensions and launches forward with a charged dash (the jump key).
 */
public abstract class AbstractNautilus extends TamableAnimal implements Saddleable, PlayerRideableJumping {
    private static final EntityDataAccessor<Boolean> SADDLED = SynchedEntityData.defineId(AbstractNautilus.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DASH_COOLDOWN = SynchedEntityData.defineId(AbstractNautilus.class, EntityDataSerializers.INT);
    public static final int DASH_COOLDOWN_TICKS = 40;
    /** Underwater the rider reaches 6.5 blocks per second, on land 0.88. */
    private static final double MAX_WATER_SPEED = 6.5 / 20.0;
    private static final double MAX_LAND_SPEED = 0.88 / 20.0;
    private static final float TAMING_CHANCE = 1.0F / 3.0F;

    public final AnimationState swimAnimationState = new AnimationState();
    public final AnimationState dashAnimationState = new AnimationState();
    private double ridingSpeed;
    private int pendingDashPower;
    private int targetMemoryTicks;

    protected AbstractNautilus(EntityType<? extends AbstractNautilus> type, Level level) {
        super(type, level);
        this.moveControl = new SmoothSwimmingMoveControl(this, 85, 10, 0.02F, 0.1F, true);
        this.lookControl = new SmoothSwimmingLookControl(this, 10);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 15.0)
                .add(Attributes.MOVEMENT_SPEED, 1.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.FOLLOW_RANGE, 16.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    public static boolean checkSpawnRules(EntityType<? extends AbstractNautilus> type, net.minecraft.world.level.LevelAccessor level,
                                          net.minecraft.world.entity.MobSpawnType spawnType, BlockPos pos, net.minecraft.util.RandomSource random) {
        return pos.getY() >= 38 && pos.getY() <= 58 && level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)
                && level.getFluidState(pos.above()).is(net.minecraft.tags.FluidTags.WATER);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new DashAttackGoal());
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.2, Ingredient.of(ModTags.Items.NAUTILUS_FOOD), false));
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 1.0, 10));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Pufferfish.class, 20, true, false,
                fish -> !isBaby() && !isVehicle()));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WaterBoundPathNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SADDLED, false);
        builder.define(DASH_COOLDOWN, 0);
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.isUnobstructed(this);
    }

    @Override
    public int getMaxAirSupply() {
        return 300;
    }

    /** Aquatic mobs (not undead ones) slowly suffocate out of water. */
    protected boolean suffocatesOnLand() {
        return true;
    }

    @Override
    public void baseTick() {
        int air = getAirSupply();
        super.baseTick();
        if (!suffocatesOnLand() || !isAlive()) return;
        if (isInWaterOrBubble()) {
            setAirSupply(getMaxAirSupply());
        } else {
            setAirSupply(air - 1);
            if (getAirSupply() == -20) {
                setAirSupply(0);
                hurt(damageSources().drown(), 2.0F);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        int cooldown = entityData.get(DASH_COOLDOWN);
        if (!level().isClientSide && cooldown > 0) {
            entityData.set(DASH_COOLDOWN, cooldown - 1);
            if (cooldown == 1 && isVehicle()) playSound(sound(isInWater() ? "dash_ready" : "dash_ready_land"), 1.0F, 1.0F);
        }
        if (level().isClientSide) {
            swimAnimationState.animateWhen(isInWater() && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4, tickCount);
        } else {
            if (getControllingPassenger() instanceof Player player) {
                player.addEffect(new MobEffectInstance(ModEffects.BREATH_OF_THE_NAUTILUS, 30, 0, true, true));
                if (isInWater() && tickCount % 40 == 0) playSound(ModSounds.event("entity.nautilus.riding").get(), 0.6F, 1.0F);
            }
            if (getTarget() != null && --targetMemoryTicks <= 0 && !(getTarget() instanceof Pufferfish)) setTarget(null);
        }
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        targetMemoryTicks = 200;
    }

    // ------------------------------------------------------------------ taming, feeding, equipment

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.Items.NAUTILUS_FOOD);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isBaby()) return super.mobInteract(player, hand);

        if (!isTame() && stack.is(ModTags.Items.NAUTILUS_TAMING_ITEMS)) {
            if (!level().isClientSide) {
                feed(player, hand, stack);
                if (random.nextFloat() < TAMING_CHANCE && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                    tame(player);
                    setTarget(null);
                    level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isTame() && isOwnedBy(player)) {
            if (stack.is(Items.SADDLE) && isSaddleable() && !isSaddled()) {
                if (!level().isClientSide) {
                    equipSaddle(stack.split(1), SoundSource.NEUTRAL);
                    gameEvent(GameEvent.EQUIP, player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (stack.getItem() instanceof NautilusArmorItem && getBodyArmorItem().isEmpty()) {
                if (!level().isClientSide) {
                    setBodyArmorItem(stack.split(1));
                    playSound(ModSounds.event("item.armor.equip_nautilus").get(), 1.0F, 1.0F);
                    gameEvent(GameEvent.EQUIP, player);
                }
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
            if (stack.is(Items.SHEARS) && (!getBodyArmorItem().isEmpty() || isSaddled())) {
                if (!level().isClientSide) shearEquipment(player, hand, stack);
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        }
        if (isFood(stack) && getHealth() < getMaxHealth()) {
            if (!level().isClientSide) {
                feed(player, hand, stack);
                heal(2.0F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isFood(stack)) return super.mobInteract(player, hand);
        if (isTame() && isSaddled() && !player.isSecondaryUseActive() && !isVehicle()) {
            if (!level().isClientSide) player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    /** Eats one item; buckets of fish leave a water bucket behind. */
    private void feed(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BucketItem) {
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
        } else {
            stack.consume(1, player);
        }
        playSound(sound("eat"), 1.0F, 1.0F);
        gameEvent(GameEvent.EAT, player);
    }

    @Override
    protected void usePlayerItem(Player player, InteractionHand hand, ItemStack stack) {
        if (stack.getItem() instanceof net.minecraft.world.item.BucketItem) {
            if (!player.getAbilities().instabuild) player.setItemInHand(hand, new ItemStack(Items.WATER_BUCKET));
        } else {
            super.usePlayerItem(player, hand, stack);
        }
    }

    private void shearEquipment(Player player, InteractionHand hand, ItemStack shears) {
        if (!getBodyArmorItem().isEmpty()) {
            spawnAtLocation(getBodyArmorItem().copy());
            setBodyArmorItem(ItemStack.EMPTY);
            playSound(ModSounds.event("item.armor.unequip_nautilus").get(), 1.0F, 1.0F);
        } else {
            spawnAtLocation(new ItemStack(Items.SADDLE));
            entityData.set(SADDLED, false);
            playSound(ModSounds.event("item.saddle.unequip").get(), 1.0F, 1.0F);
        }
        shears.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        gameEvent(GameEvent.SHEAR, player);
    }

    @Override
    public boolean isSaddleable() {
        return isAlive() && !isBaby() && isTame();
    }

    @Override
    public void equipSaddle(ItemStack stack, @Nullable SoundSource source) {
        entityData.set(SADDLED, true);
        if (source != null) {
            level().playSound(null, this, ModSounds.event(isInWater() ? "item.nautilus_saddle_underwater_equip" : "item.nautilus_saddle_equip").get(),
                    source, 1.0F, 1.0F);
        }
    }

    @Override
    public boolean isSaddled() {
        return entityData.get(SADDLED);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.BODY ? isTame() && !isBaby() : super.canUseSlot(slot);
    }

    @Override
    public boolean isBodyArmorItem(ItemStack stack) {
        return stack.getItem() instanceof NautilusArmorItem;
    }

    @Override
    protected void dropEquipment() {
        super.dropEquipment();
        if (isSaddled()) spawnAtLocation(Items.SADDLE);
    }

    @Override
    public boolean canBeLeashed() {
        return getTarget() == null && super.canBeLeashed();
    }

    // ------------------------------------------------------------------ riding

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        if (isSaddled() && getFirstPassenger() instanceof Player player) return player;
        return super.getControllingPassenger();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        return new Vec3(player.xxa * 0.5F, 0.0, player.zza);
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        setRot(player.getYRot(), player.getXRot() * 0.5F);
        yRotO = yBodyRot = yHeadRot = getYRot();
        double max = isInWater() ? MAX_WATER_SPEED : MAX_LAND_SPEED;
        ridingSpeed = travelVector.z > 0 ? Math.min(max, ridingSpeed + max * 0.04) : Math.max(0.0, ridingSpeed - max * 0.1);
        if (pendingDashPower > 0) {
            dash(player, pendingDashPower);
            pendingDashPower = 0;
        }
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return (float) ridingSpeed;
    }

    @Override
    public void travel(Vec3 input) {
        if (isControlledByLocalInstance() && getControllingPassenger() instanceof Player player) {
            Vec3 look = Vec3.directionFromRotation(isInWater() ? player.getXRot() : 0.0F, player.getYRot());
            Vec3 side = Vec3.directionFromRotation(0.0F, player.getYRot() - 90.0F);
            Vec3 wanted = look.scale(Math.signum(input.z) * ridingSpeed).add(side.scale(input.x * ridingSpeed));
            if (isInWater()) {
                setDeltaMovement(getDeltaMovement().scale(0.9).add(wanted.scale(0.1 * 2)));
                move(MoverType.SELF, getDeltaMovement());
            } else {
                super.travel(new Vec3(input.x, 0.0, input.z));
            }
            calculateEntityAnimation(false);
            return;
        }
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), input);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9));
            if (getTarget() == null) setDeltaMovement(getDeltaMovement().add(0.0, -0.005, 0.0));
        } else {
            super.travel(input);
        }
    }

    /** Launches the nautilus along the rider's view; a full charge travels about twelve blocks underwater. */
    private void dash(Player player, int power) {
        float strength = Mth.clamp(power / 100.0F, 0.2F, 1.0F);
        Vec3 look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
        double speed = isInWater() ? 1.2 * strength : 0.5 * strength;
        setDeltaMovement(getDeltaMovement().add(look.scale(speed)));
        ridingSpeed = isInWater() ? MAX_WATER_SPEED : MAX_LAND_SPEED;
    }

    @Override
    public void onPlayerJump(int jumpPower) {
        if (isSaddled() && entityData.get(DASH_COOLDOWN) <= 0) pendingDashPower = Math.max(jumpPower, 1);
    }

    @Override
    public boolean canJump() {
        return isSaddled();
    }

    @Override
    public void handleStartJump(int jumpPower) {
        if (entityData.get(DASH_COOLDOWN) > 0) return;
        entityData.set(DASH_COOLDOWN, DASH_COOLDOWN_TICKS);
        playSound(sound(isInWater() ? "dash" : "dash_land"), 1.0F, 1.0F);
        level().broadcastEntityEvent(this, (byte) 61);
        gameEvent(GameEvent.ENTITY_ACTION);
    }

    @Override
    public void handleStopJump() {
    }

    @Override
    public int getJumpCooldown() {
        return entityData.get(DASH_COOLDOWN);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 61) {
            dashAnimationState.start(tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, dimensions.height() * 0.85, 0.0);
    }

    @Override
    public boolean canBeCollidedWith() {
        return isVehicle();
    }

    // ------------------------------------------------------------------ sounds

    /** Sound {@code entity.<type>.<name>}, with the baby variants for nautilus babies. */
    protected abstract SoundEvent sound(String name);

    private String landSuffix() {
        return isInWater() ? "" : "_land";
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return sound("ambient" + landSuffix());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sound("hurt" + landSuffix());
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sound("death" + landSuffix());
    }

    @Override
    protected SoundEvent getSwimSound() {
        return sound("swim");
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    }

    @Override
    public SoundEvent getSaddleSoundEvent() {
        return ModSounds.event("item.nautilus_saddle_equip").get();
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("saddle", isSaddled());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(SADDLED, tag.getBoolean("saddle"));
    }

    @Override
    public boolean canMate(net.minecraft.world.entity.animal.Animal other) {
        return other != this && other.getClass() == getClass() && isInLove() && other.isInLove();
    }

    /** Dashes into its target (pufferfish it hunts or whoever hurt it) and bites on contact. */
    private final class DashAttackGoal extends Goal {
        private int cooldown;
        private int dashTicks;

        DashAttackGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = getTarget();
            return target != null && target.isAlive() && !isBaby() && getControllingPassenger() == null;
        }

        @Override
        public void stop() {
            dashTicks = 0;
            getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = getTarget();
            if (target == null) return;
            getLookControl().setLookAt(target, 30.0F, 30.0F);
            if (cooldown > 0) cooldown--;
            if (dashTicks > 0) {
                dashTicks--;
                if (getBoundingBox().inflate(0.3).intersects(target.getBoundingBox())) {
                    doHurtTarget(target);
                    dashTicks = 0;
                }
                return;
            }
            double distance = distanceToSqr(target);
            if (cooldown <= 0 && distance < 64.0 && hasLineOfSight(target)) {
                Vec3 toward = target.getEyePosition().subtract(position()).normalize();
                setDeltaMovement(toward.scale(isInWater() ? 1.0 : 0.6));
                dashTicks = 10;
                cooldown = DASH_COOLDOWN_TICKS;
                playSound(sound(isInWater() ? "dash" : "dash_land"), 1.0F, 1.0F);
                level().broadcastEntityEvent(AbstractNautilus.this, (byte) 61);
            } else {
                getNavigation().moveTo(target, 1.2);
            }
        }
    }
}
