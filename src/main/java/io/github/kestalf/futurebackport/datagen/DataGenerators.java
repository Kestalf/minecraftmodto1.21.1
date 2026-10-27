package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = FutureBackport.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class DataGenerators {
    private static final RegistrySetBuilder REGISTRIES = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, ModConfiguredFeatures::bootstrap);

    @SubscribeEvent
    static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper files = event.getExistingFileHelper();

        DatapackBuiltinEntriesProvider datapack = generator.addProvider(event.includeServer(),
                new DatapackBuiltinEntriesProvider(output, event.getLookupProvider(), REGISTRIES, Set.of(FutureBackport.MOD_ID)));
        CompletableFuture<HolderLookup.Provider> lookup = datapack.getRegistryProvider();

        generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLootProvider::new, LootContextParamSets.BLOCK)), lookup));
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup));
        ModBlockTagsProvider blockTags = generator.addProvider(event.includeServer(), new ModBlockTagsProvider(output, lookup, files));
        generator.addProvider(event.includeServer(), new ModItemTagsProvider(output, lookup, blockTags.contentsGetter(), files));
        generator.addProvider(event.includeServer(), new ModDataMapProvider(output, lookup));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, files));
    }

    private DataGenerators() {
    }
}
