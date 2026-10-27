package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = FutureBackport.MOD_ID)
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, FutureBackport.MOD_ID);

    /** Given to nautilus riders: the air supply neither drops nor refills while it lasts. */
    public static final DeferredHolder<MobEffect, MobEffect> BREATH_OF_THE_NAUTILUS = EFFECTS.register("breath_of_the_nautilus",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x00D2D2) {
            });

    @SubscribeEvent
    static void onBreathe(LivingBreatheEvent event) {
        if (!event.canBreathe() && event.getEntity().hasEffect(BREATH_OF_THE_NAUTILUS)) event.setConsumeAirAmount(0);
    }

    private ModEffects() {
    }
}
