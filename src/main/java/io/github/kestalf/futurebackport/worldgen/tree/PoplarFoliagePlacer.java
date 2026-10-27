package io.github.kestalf.futurebackport.worldgen.tree;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kestalf.futurebackport.worldgen.ModWorldgen;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;

/** Tall, narrow, slightly bulging crown of a poplar tree. */
public class PoplarFoliagePlacer extends FoliagePlacer {
    public static final MapCodec<PoplarFoliagePlacer> CODEC = RecordCodecBuilder.mapCodec(instance -> foliagePlacerParts(instance)
            .and(IntProvider.codec(1, 16).fieldOf("height").forGetter(p -> p.height))
            .apply(instance, PoplarFoliagePlacer::new));
    private final IntProvider height;

    public PoplarFoliagePlacer(IntProvider radius, IntProvider offset, IntProvider height) {
        super(radius, offset);
        this.height = height;
    }

    @Override
    protected FoliagePlacerType<?> type() {
        return ModWorldgen.POPLAR_FOLIAGE_PLACER.get();
    }

    @Override
    protected void createFoliage(LevelSimulatedReader level, FoliageSetter setter, RandomSource random, TreeConfiguration config,
                                 int maxFreeTreeHeight, FoliageAttachment attachment, int foliageHeight, int foliageRadius, int offset) {
        // From the top (offset) down: a single-block tip, then the widest part in the middle and a narrower base.
        for (int y = offset; y >= offset - foliageHeight; y--) {
            int fromTop = offset - y;
            int radius;
            if (fromTop == 0) radius = 0;
            else if (fromTop == 1 || y == offset - foliageHeight) radius = Math.max(foliageRadius - 1, 1);
            else radius = foliageRadius;
            placeLeavesRow(level, setter, random, config, attachment.pos(), radius, y, attachment.doubleTrunk());
        }
    }

    @Override
    public int foliageHeight(RandomSource random, int treeHeight, TreeConfiguration config) {
        return height.sample(random);
    }

    @Override
    protected boolean shouldSkipLocation(RandomSource random, int localX, int localY, int localZ, int range, boolean large) {
        return range > 0 && localX == range && localZ == range;
    }
}
