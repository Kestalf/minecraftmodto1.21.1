package io.github.kestalf.futurebackport.client;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.ghast.HappyGhast;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;

/** Client game bus listeners. */
@EventBusSubscriber(modid = FutureBackport.MOD_ID, value = Dist.CLIENT)
public final class ClientGameEvents {
    /** The third-person camera is twice as far away while riding a happy ghast. */
    @SubscribeEvent
    static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        Entity entity = event.getCamera().getEntity();
        if (entity != null && entity.getVehicle() instanceof HappyGhast) {
            event.setDistance(event.getDistance() * 2.0F);
        }
    }

    private ClientGameEvents() {
    }
}
