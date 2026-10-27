package io.github.kestalf.futurebackport.mixin;

import io.github.kestalf.futurebackport.entity.golem.CopperGolemBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CarvedPumpkinBlock.class)
public abstract class CarvedPumpkinBlockMixin {
    /** A carved pumpkin placed next to a block of copper builds a copper golem. */
    @Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
    private void futurebackport$spawnCopperGolem(Level level, BlockPos pos, CallbackInfo ci) {
        if (CopperGolemBuilding.trySpawn(level, pos)) ci.cancel();
    }

    /** Lets dispensers place pumpkins that complete a copper golem. */
    @Inject(method = "canSpawnGolem", at = @At("RETURN"), cancellable = true)
    private void futurebackport$canSpawnCopperGolem(LevelReader level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && CopperGolemBuilding.findCopper(level, pos) != null) cir.setReturnValue(true);
    }
}
