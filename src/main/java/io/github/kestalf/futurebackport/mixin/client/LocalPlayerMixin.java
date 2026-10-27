package io.github.kestalf.futurebackport.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.kestalf.futurebackport.item.spear.SpearItem;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    /** Charging with a spear neither slows the player down nor prevents sprinting. */
    @WrapOperation(method = {"aiStep", "canStartSprinting"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean futurebackport$spearKeepsSpeed(LocalPlayer player, Operation<Boolean> original) {
        return original.call(player) && !(player.getUseItem().getItem() instanceof SpearItem);
    }
}
