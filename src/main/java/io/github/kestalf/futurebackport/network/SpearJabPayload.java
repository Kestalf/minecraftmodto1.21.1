package io.github.kestalf.futurebackport.network;

import io.github.kestalf.futurebackport.FutureBackport;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Sent by the client when the player jabs with a spear: the entities in reach and whether Lunge was applied. */
public record SpearJabPayload(List<Integer> targets, boolean lunge) implements CustomPacketPayload {
    public static final Type<SpearJabPayload> TYPE = new Type<>(FutureBackport.id("spear_jab"));
    public static final StreamCodec<ByteBuf, SpearJabPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(16)), SpearJabPayload::targets,
            ByteBufCodecs.BOOL, SpearJabPayload::lunge,
            SpearJabPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
