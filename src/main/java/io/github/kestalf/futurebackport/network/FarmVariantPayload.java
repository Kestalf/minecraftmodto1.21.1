package io.github.kestalf.futurebackport.network;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.variant.FarmAnimalVariant;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Tells clients the variant of a chicken, cow or pig (attachments are not synced in this version). */
public record FarmVariantPayload(int entityId, FarmAnimalVariant variant) implements CustomPacketPayload {
    public static final Type<FarmVariantPayload> TYPE = new Type<>(FutureBackport.id("farm_variant"));
    public static final StreamCodec<FriendlyByteBuf, FarmVariantPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FarmVariantPayload::entityId,
            ByteBufCodecs.VAR_INT.map(i -> FarmAnimalVariant.values()[i], FarmAnimalVariant::ordinal), FarmVariantPayload::variant,
            FarmVariantPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
