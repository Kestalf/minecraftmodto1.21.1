package io.github.kestalf.futurebackport.mixin;

import io.github.kestalf.futurebackport.entity.desert.CamelHusk;
import io.github.kestalf.futurebackport.entity.desert.Parched;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModItems;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieMixin extends Monster {
    private ZombieMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** One in ten naturally spawned adult husks ride a camel husk, with an iron spear and a parched behind them. */
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void futurebackport$spawnCamelHuskJockey(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                                     SpawnGroupData spawnGroupData, CallbackInfoReturnable<SpawnGroupData> cir) {
        if (!((Object) this instanceof Husk) || spawnType != MobSpawnType.NATURAL || isBaby() || isPassenger()
                || getRandom().nextFloat() >= 0.1F) {
            return;
        }
        CamelHusk camel = ModEntities.CAMEL_HUSK.get().create(level.getLevel());
        if (camel == null) return;
        camel.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
        if (!level.noCollision(camel)) return;
        camel.finalizeSpawn(level, difficulty, MobSpawnType.JOCKEY, null);
        level.addFreshEntity(camel);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.IRON_SPEAR.get()));
        startRiding(camel, true);
        Parched parched = ModEntities.PARCHED.get().create(level.getLevel());
        if (parched != null) {
            parched.moveTo(getX(), getY(), getZ(), getYRot(), 0.0F);
            parched.finalizeSpawn(level, difficulty, MobSpawnType.JOCKEY, null);
            parched.startRiding(camel, true);
            level.addFreshEntity(parched);
        }
    }
}
