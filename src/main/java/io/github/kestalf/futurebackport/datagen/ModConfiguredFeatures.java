package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.worldgen.ModFeatureKeys;
import io.github.kestalf.futurebackport.worldgen.tree.PaleMossDecorator;
import io.github.kestalf.futurebackport.worldgen.tree.PoplarFoliagePlacer;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.ThreeLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.DarkOakFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.DarkOakTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;

import java.util.List;
import java.util.OptionalInt;

public final class ModConfiguredFeatures {
    public static void bootstrap(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        HolderGetter<ConfiguredFeature<?, ?>> features = context.lookup(Registries.CONFIGURED_FEATURE);

        register(context, ModFeatureKeys.PALE_OAK_BONEMEAL, Feature.TREE, paleOak().build());
        register(context, ModFeatureKeys.PALE_OAK, Feature.TREE, paleOak()
                .decorators(List.of(new PaleMossDecorator(0.15F, 0.4F, 0.8F))).build());
        register(context, ModFeatureKeys.PALE_OAK_CREAKING, Feature.TREE, paleOak()
                .decorators(List.of(new PaleMossDecorator(0.15F, 0.4F, 0.8F))).build());

        WoodFamily poplar = ModBlocks.POPLAR;
        register(context, ModFeatureKeys.RED_POPLAR, Feature.TREE, poplar(poplar.leaves.get(0).get()).build());
        register(context, ModFeatureKeys.ORANGE_POPLAR, Feature.TREE, poplar(poplar.leaves.get(1).get()).build());
        register(context, ModFeatureKeys.YELLOW_POPLAR, Feature.TREE, poplar(poplar.leaves.get(2).get()).build());
        register(context, ModFeatureKeys.POPLAR, Feature.RANDOM_SELECTOR, new RandomFeatureConfiguration(List.of(
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(ModFeatureKeys.RED_POPLAR)), 0.33F),
                new WeightedPlacedFeature(PlacementUtils.inlinePlaced(features.getOrThrow(ModFeatureKeys.ORANGE_POPLAR)), 0.5F)),
                PlacementUtils.inlinePlaced(features.getOrThrow(ModFeatureKeys.YELLOW_POPLAR))));

        var vegetation = PlacementUtils.inlinePlaced(Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(new WeightedStateProvider(
                SimpleWeightedRandomList.<net.minecraft.world.level.block.state.BlockState>builder()
                        .add(Blocks.SHORT_GRASS.defaultBlockState(), 10)
                        .add(ModBlocks.PALE_MOSS_CARPET.get().defaultBlockState(), 5)
                        .add(Blocks.TALL_GRASS.defaultBlockState(), 2))));
        register(context, ModFeatureKeys.PALE_MOSS_PATCH_BONEMEAL, Feature.VEGETATION_PATCH, new VegetationPatchConfiguration(
                BlockTags.MOSS_REPLACEABLE, BlockStateProvider.simple(ModBlocks.PALE_MOSS_BLOCK.get()), vegetation, CaveSurface.FLOOR,
                ConstantInt.of(1), 0.0F, 5, 0.6F, UniformInt.of(1, 2), 0.75F));
        register(context, ModFeatureKeys.PALE_MOSS_PATCH, Feature.VEGETATION_PATCH, new VegetationPatchConfiguration(
                BlockTags.MOSS_REPLACEABLE, BlockStateProvider.simple(ModBlocks.PALE_MOSS_BLOCK.get()), vegetation, CaveSurface.FLOOR,
                ConstantInt.of(1), 0.0F, 5, 0.3F, UniformInt.of(2, 4), 0.75F));
    }

    private static TreeConfiguration.TreeConfigurationBuilder paleOak() {
        WoodFamily f = ModBlocks.PALE_OAK;
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(f.log.get()), new DarkOakTrunkPlacer(6, 2, 1),
                BlockStateProvider.simple(f.leaves.getFirst().get()), new DarkOakFoliagePlacer(ConstantInt.of(0), ConstantInt.of(0)),
                new ThreeLayersFeatureSize(1, 1, 0, 1, 2, OptionalInt.empty())).ignoreVines();
    }

    private static TreeConfiguration.TreeConfigurationBuilder poplar(Block leaves) {
        return new TreeConfiguration.TreeConfigurationBuilder(
                BlockStateProvider.simple(ModBlocks.POPLAR.log.get()), new StraightTrunkPlacer(7, 3, 1),
                BlockStateProvider.simple(leaves), new PoplarFoliagePlacer(ConstantInt.of(2), ConstantInt.of(1), UniformInt.of(6, 8)),
                new TwoLayersFeatureSize(1, 0, 1)).ignoreVines();
    }

    private static <FC extends FeatureConfiguration, F extends Feature<FC>> void register(
            BootstrapContext<ConfiguredFeature<?, ?>> context, ResourceKey<ConfiguredFeature<?, ?>> key, F feature, FC config) {
        context.register(key, new ConfiguredFeature<>(feature, config));
    }

    private ModConfiguredFeatures() {
    }
}
