package io.github.kestalf.futurebackport.client;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.client.resources.LegacyStuffWrapper;

import java.io.IOException;

/** The "dry foliage" color map of 1.21.5+, used to tint leaf litter by biome. */
public final class DryFoliageColor {
    private static final ResourceLocation LOCATION = FutureBackport.id("textures/colormap/dry_foliage.png");
    public static final int DEFAULT = 0xFFA0814F;
    private static int[] pixels = new int[65536];

    public static final ColorResolver RESOLVER = (biome, x, z) -> get(
            Mth.clamp(biome.getBaseTemperature(), 0.0F, 1.0F),
            Mth.clamp(biome.getModifiedClimateSettings().downfall(), 0.0F, 1.0F));

    public static int get(double temperature, double downfall) {
        downfall *= temperature;
        int x = (int) ((1.0 - temperature) * 255.0);
        int y = (int) ((1.0 - downfall) * 255.0);
        int index = y << 8 | x;
        return index >= pixels.length || pixels[index] == 0 ? DEFAULT : pixels[index];
    }

    private DryFoliageColor() {
    }

    static final class ReloadListener extends SimplePreparableReloadListener<int[]> {
        @Override
        protected int[] prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
            try {
                return LegacyStuffWrapper.getPixels(resourceManager, LOCATION);
            } catch (IOException e) {
                FutureBackport.LOGGER.warn("Could not load the dry foliage color map", e);
                return new int[65536];
            }
        }

        @Override
        protected void apply(int[] loaded, ResourceManager resourceManager, ProfilerFiller profiler) {
            pixels = loaded;
        }
    }
}
