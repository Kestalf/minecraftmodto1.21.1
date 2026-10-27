package io.github.kestalf.futurebackport.mixin;

import io.github.kestalf.futurebackport.entity.variant.FarmVariantEvents;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEgg.class)
public abstract class ThrownEggMixin {
    /** Chicks hatching from a blue or brown egg are cold or warm. */
    @Inject(method = "onHit", at = @At("HEAD"))
    private void futurebackport$startHatching(HitResult result, CallbackInfo ci) {
        FarmVariantEvents.startHatching(((ThrownEgg) (Object) this).getItem());
    }

    @Inject(method = "onHit", at = @At("RETURN"))
    private void futurebackport$stopHatching(HitResult result, CallbackInfo ci) {
        FarmVariantEvents.stopHatching();
    }
}
