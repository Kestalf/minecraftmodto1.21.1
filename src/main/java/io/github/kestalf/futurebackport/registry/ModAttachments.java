package io.github.kestalf.futurebackport.registry;

import com.mojang.serialization.Codec;
import io.github.kestalf.futurebackport.FutureBackport;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FutureBackport.MOD_ID);

    /** Set on baby mobs by a golden dandelion: they stop growing up ("AgeLocked" in vanilla). */
    public static final Supplier<AttachmentType<Boolean>> AGE_LOCKED = ATTACHMENT_TYPES.register("age_locked",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    private ModAttachments() {
    }
}
