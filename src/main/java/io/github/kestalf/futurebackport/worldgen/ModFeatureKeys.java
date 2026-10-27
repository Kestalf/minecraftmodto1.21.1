package io.github.kestalf.futurebackport.worldgen;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Keys of the configured and placed features defined in {@code ModWorldgenProvider}. */
public final class ModFeatureKeys {
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_OAK = configured("pale_oak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_OAK_BONEMEAL = configured("pale_oak_bonemeal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_OAK_CREAKING = configured("pale_oak_creaking");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_MOSS_PATCH = configured("pale_moss_patch");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALE_MOSS_PATCH_BONEMEAL = configured("pale_moss_patch_bonemeal");
    public static final ResourceKey<ConfiguredFeature<?, ?>> POPLAR = configured("poplar");
    public static final ResourceKey<ConfiguredFeature<?, ?>> RED_POPLAR = configured("red_poplar");
    public static final ResourceKey<ConfiguredFeature<?, ?>> ORANGE_POPLAR = configured("orange_poplar");
    public static final ResourceKey<ConfiguredFeature<?, ?>> YELLOW_POPLAR = configured("yellow_poplar");

    public static ResourceKey<ConfiguredFeature<?, ?>> configured(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, FutureBackport.id(name));
    }

    public static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE, FutureBackport.id(name));
    }

    private ModFeatureKeys() {
    }
}
