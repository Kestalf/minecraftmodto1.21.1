package io.github.kestalf.futurebackport.registry;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.loot.AddItemLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Global loot modifier types (the additions themselves are generated in datagen). */
public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, FutureBackport.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<AddItemLootModifier>> ADD_ITEM =
            SERIALIZERS.register("add_item", () -> AddItemLootModifier.CODEC);

    private ModLootModifiers() {
    }
}
