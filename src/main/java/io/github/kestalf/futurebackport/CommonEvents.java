package io.github.kestalf.futurebackport;

import io.github.kestalf.futurebackport.entity.creaking.Creaking;
import io.github.kestalf.futurebackport.item.spear.SpearCombat;
import io.github.kestalf.futurebackport.item.spear.SpearItem;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Game (not mod) bus listeners shared by both sides. */
@EventBusSubscriber(modid = FutureBackport.MOD_ID)
public final class CommonEvents {
    /** Spears never deal critical hits. */
    @SubscribeEvent
    static void onCriticalHit(CriticalHitEvent event) {
        if (event.getEntity().getMainHandItem().getItem() instanceof SpearItem) event.setCriticalHit(false);
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!event.getEntity().level().isClientSide) SpearCombat.trackPosition(event.getEntity());
    }

    /** Illagers flee from creakings (ravagers do not). */
    @SubscribeEvent
    static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof AbstractIllager illager) {
            illager.goalSelector.addGoal(3, new AvoidEntityGoal<>(illager, Creaking.class, 8.0F, 1.0, 1.2));
        }
    }

    private CommonEvents() {
    }
}
