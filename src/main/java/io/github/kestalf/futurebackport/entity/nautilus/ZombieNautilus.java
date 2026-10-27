package io.github.kestalf.futurebackport.entity.nautilus;

import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import org.jetbrains.annotations.Nullable;

/**
 * Undead nautilus, usually ridden by a trident-wielding drowned. It cannot breed, does not suffocate on land and
 * burns in daylight. Those from warm oceans are overgrown with coral.
 */
public class ZombieNautilus extends AbstractNautilus {
    private static final EntityDataAccessor<Boolean> CORAL = SynchedEntityData.defineId(ZombieNautilus.class, EntityDataSerializers.BOOLEAN);

    public ZombieNautilus(EntityType<? extends ZombieNautilus> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CORAL, false);
    }

    public boolean isCoral() {
        return entityData.get(CORAL);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        entityData.set(CORAL, level.getBiome(blockPosition()).is(Biomes.WARM_OCEAN));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    @Override
    protected boolean suffocatesOnLand() {
        return false;
    }

    @Override
    public void aiStep() {
        if (isAlive() && isSunBurnTick() && !isInWaterRainOrBubble()) igniteForSeconds(8.0F);
        super.aiStep();
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    @Override
    protected SoundEvent sound(String name) {
        String key = name.equals("riding") ? "entity.nautilus.riding" : "entity.zombie_nautilus." + name;
        return ModSounds.event(key).get();
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("coral", isCoral());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(CORAL, tag.getBoolean("coral"));
    }
}
