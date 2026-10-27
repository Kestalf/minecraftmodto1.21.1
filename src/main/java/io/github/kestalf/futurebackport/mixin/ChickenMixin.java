package io.github.kestalf.futurebackport.mixin;

import io.github.kestalf.futurebackport.entity.variant.FarmVariantEvents;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Chicken.class)
public abstract class ChickenMixin {
    /** Warm chickens lay brown eggs, cold ones blue eggs. */
    @ModifyArg(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/Chicken;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemLike futurebackport$variantEgg(ItemLike egg) {
        return FarmVariantEvents.eggFor((Chicken) (Object) this);
    }
}
