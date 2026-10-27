package io.github.kestalf.futurebackport.entity.desert;

import com.google.common.collect.ImmutableList;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.camel.Camel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Undead camel of the desert, usually ridden by a husk with a spear and a parched. It cannot breed, is tempted and
 * healed with rabbit's feet, slowly regenerates and takes half fall damage from six blocks on.
 */
public class CamelHusk extends Camel {
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
            MemoryModuleType.IS_PANICKING, MemoryModuleType.HURT_BY, MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.WALK_TARGET,
            MemoryModuleType.LOOK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryModuleType.TEMPTING_PLAYER, MemoryModuleType.TEMPTATION_COOLDOWN_TICKS,
            MemoryModuleType.GAZE_COOLDOWN_TICKS, MemoryModuleType.IS_TEMPTED, MemoryModuleType.BREED_TARGET,
            MemoryModuleType.NEAREST_VISIBLE_ADULT);

    public CamelHusk(EntityType<? extends Camel> type, Level level) {
        super(type, level);
    }

    @Override
    protected Brain.Provider<Camel> brainProvider() {
        ImmutableList<SensorType<? extends Sensor<? super Camel>>> sensors = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES,
                SensorType.HURT_BY, ModEntities.CAMEL_HUSK_TEMPTATIONS.get(), SensorType.NEAREST_ADULT);
        return Brain.provider(MEMORY_TYPES, sensors);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.RABBIT_FOOT);
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Override
    public boolean canMate(Animal other) {
        return false;
    }

    @Override
    @Nullable
    public Camel getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && isAlive() && getHealth() < getMaxHealth() && random.nextInt(900) == 0) heal(1.0F);
    }

    @Override
    protected int calculateFallDamage(float fallDistance, float damageMultiplier) {
        return Mth.ceil((fallDistance - 6.0F) * 0.5F * damageMultiplier);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !isSaddled() && !isVehicle();
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return true;
    }

    // ------------------------------------------------------------------ the camel's sounds, as camel husk sounds

    private static SoundEvent sound(String name) {
        return ModSounds.event("entity.camel_husk." + name).get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return sound("ambient");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sound("death");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sound("hurt");
    }

    @Override
    protected SoundEvent getEatingSound() {
        return sound("eat");
    }

    @Override
    public SoundEvent getSaddleSoundEvent() {
        return sound("saddle");
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(state.is(BlockTags.CAMEL_SAND_STEP_SOUND_BLOCKS) ? sound("step_sand") : sound("step"), 1.0F, 1.0F);
    }

    @Override
    public void handleStartJump(int jumpPower) {
        makeSound(sound("dash"));
        gameEvent(GameEvent.ENTITY_ACTION);
        setDashing(true);
    }

    @Override
    public void sitDown() {
        if (isCamelSitting()) return;
        makeSound(sound("sit"));
        setPose(Pose.SITTING);
        gameEvent(GameEvent.ENTITY_ACTION);
        resetLastPoseChangeTick(-level().getGameTime());
    }

    @Override
    public void standUp() {
        if (!isCamelSitting()) return;
        makeSound(sound("stand"));
        setPose(Pose.STANDING);
        gameEvent(GameEvent.ENTITY_ACTION);
        resetLastPoseChangeTick(level().getGameTime());
    }
}
