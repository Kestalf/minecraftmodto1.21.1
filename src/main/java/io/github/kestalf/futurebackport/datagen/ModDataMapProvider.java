package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.block.CopperFamily;
import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.Oxidizable;
import net.neoforged.neoforge.registries.datamaps.builtin.Strippable;
import net.neoforged.neoforge.registries.datamaps.builtin.Waxable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModDataMapProvider extends DataMapProvider {
    public ModDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup);
    }

    @Override
    protected void gather() {
        var strippables = builder(NeoForgeDataMaps.STRIPPABLES);
        for (WoodFamily f : List.of(ModBlocks.PALE_OAK, ModBlocks.POPLAR)) {
            strippables.add(f.log, new Strippable(f.strippedLog.get()), false);
            strippables.add(f.wood, new Strippable(f.strippedWood.get()), false);
        }

        var oxidizables = builder(NeoForgeDataMaps.OXIDIZABLES);
        var waxables = builder(NeoForgeDataMaps.WAXABLES);
        CopperFamily copper = ModBlocks.COPPER;
        for (CopperFamily.Weathering<?> weathering : List.of(copper.bars, copper.chains, copper.lanterns, copper.lightningRods)) {
            WeatherState[] states = WeatherState.values();
            for (int i = 0; i < states.length; i++) {
                Block unwaxed = block(weathering, states[i]);
                if (i + 1 < states.length) {
                    oxidizables.add(unwaxed.builtInRegistryHolder(), new Oxidizable(block(weathering, states[i + 1])), false);
                }
                waxables.add(unwaxed.builtInRegistryHolder(), new Waxable(weathering.waxed.get(states[i]).get()), false);
            }
        }

        var compostables = builder(NeoForgeDataMaps.COMPOSTABLES);
        for (WoodFamily f : List.of(ModBlocks.PALE_OAK, ModBlocks.POPLAR)) {
            compost(compostables, 0.3F, f.sapling.get());
            f.leaves.forEach(l -> compost(compostables, 0.3F, l.get()));
        }
        compost(compostables, 0.3F, ModBlocks.PALE_MOSS_CARPET.get(), ModBlocks.PALE_HANGING_MOSS.get(), ModBlocks.LEAF_LITTER.get(),
                ModBlocks.WILDFLOWERS.get(), ModBlocks.BUSH.get(), ModBlocks.FIREFLY_BUSH.get(), ModBlocks.SHORT_DRY_GRASS.get(),
                ModBlocks.CACTUS_FLOWER.get(), ModBlocks.RED_SHRUB.get());
        compost(compostables, 0.5F, ModBlocks.TALL_DRY_GRASS.get());
        compost(compostables, 0.65F, ModBlocks.PALE_MOSS_BLOCK.get(), ModBlocks.OPEN_EYEBLOSSOM.get(), ModBlocks.CLOSED_EYEBLOSSOM.get(),
                ModBlocks.GOLDEN_DANDELION.get(), ModBlocks.SHELF_MUSHROOM.get());
    }

    private static Block block(CopperFamily.Weathering<?> weathering, WeatherState state) {
        return weathering.unwaxed.containsKey(state) ? weathering.unwaxed.get(state).get() : Blocks.LIGHTNING_ROD;
    }

    private static void compost(Builder<Compostable, net.minecraft.world.item.Item> builder, float chance, ItemLike... items) {
        for (ItemLike item : items) builder.add(item.asItem().builtInRegistryHolder(), new Compostable(chance), false);
    }
}
