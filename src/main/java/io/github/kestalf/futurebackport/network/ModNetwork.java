package io.github.kestalf.futurebackport.network;

import io.github.kestalf.futurebackport.item.spear.SpearCombat;
import io.github.kestalf.futurebackport.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SpearJabPayload.TYPE, SpearJabPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) SpearCombat.jab(player, payload.targets(), payload.lunge());
        });
        registrar.playToClient(FarmVariantPayload.TYPE, FarmVariantPayload.STREAM_CODEC, (payload, context) -> {
            var entity = context.player().level().getEntity(payload.entityId());
            if (entity != null) entity.setData(ModAttachments.FARM_VARIANT, payload.variant());
        });
    }

    private ModNetwork() {
    }
}
