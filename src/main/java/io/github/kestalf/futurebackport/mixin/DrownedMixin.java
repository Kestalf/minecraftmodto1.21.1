package io.github.kestalf.futurebackport.mixin;

import io.github.kestalf.futurebackport.entity.nautilus.ZombieNautilus;
import io.github.kestalf.futurebackport.registry.ModEntities;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biomes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Drowned.class)
public abstract class DrownedMixin extends Zombie {
    private DrownedMixin(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    /** Half of the adult drowned spawning with a trident outside rivers ride a zombie nautilus. */
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void futurebackport$spawnNautilusJockey(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                                     SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (spawnType == MobSpawnType.REINFORCEMENT || spawnType == MobSpawnType.CONVERSION || isBaby() || isPassenger()
                || !getMainHandItem().is(Items.TRIDENT) || getRandom().nextFloat() >= 0.5F) {
            return;
        }
        var biome = level.getBiome(blockPosition());
        if (biome.is(Biomes.RIVER) || biome.is(Biomes.FROZEN_RIVER)) return;
        ZombieNautilus nautilus = ModEntities.ZOMBIE_NAUTILUS.get().create(level.getLevel());
        if (nautilus == null) return;
        nautilus.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        nautilus.finalizeSpawn(level, difficulty, MobSpawnType.JOCKEY, null);
        startRiding(nautilus, true);
        level.addFreshEntity(nautilus);
    }
}
