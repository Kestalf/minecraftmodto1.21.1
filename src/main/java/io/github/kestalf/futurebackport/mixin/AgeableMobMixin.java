package io.github.kestalf.futurebackport.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.github.kestalf.futurebackport.registry.ModAttachments;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin {
    /** Babies whose growth was paused with a golden dandelion do not age. */
    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AgeableMob;setAge(I)V"))
    private boolean futurebackport$respectAgeLock(AgeableMob mob, int age) {
        return !(mob.isBaby() && mob.getData(ModAttachments.AGE_LOCKED));
    }
}
