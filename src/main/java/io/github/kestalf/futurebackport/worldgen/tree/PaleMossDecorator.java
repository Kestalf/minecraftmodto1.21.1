package io.github.kestalf.futurebackport.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kestalf.futurebackport.block.plant.HangingMossBlock;
import io.github.kestalf.futurebackport.block.plant.PaleMossCarpetBlock;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.worldgen.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

/**
 * Decorates pale oaks: pale hanging moss below the leaves and pale moss (with carpets) on the ground around the trunk.
 */
public class PaleMossDecorator extends TreeDecorator {
    public static final MapCodec<PaleMossDecorator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.floatRange(0, 1).fieldOf("probability").forGetter(d -> d.probability),
            Codec.floatRange(0, 1).fieldOf("trunk_probability").forGetter(d -> d.trunkProbability),
            Codec.floatRange(0, 1).fieldOf("ground_probability").forGetter(d -> d.groundProbability)
    ).apply(instance, PaleMossDecorator::new));
    private final float probability;
    private final float trunkProbability;
    private final float groundProbability;

    public PaleMossDecorator(float probability, float trunkProbability, float groundProbability) {
        this.probability = probability;
        this.trunkProbability = trunkProbability;
        this.groundProbability = groundProbability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModWorldgen.PALE_MOSS.get();
    }

    @Override
    public void place(Context context) {
        RandomSource random = context.random();
        if (random.nextFloat() >= probability || context.logs().isEmpty()) return;

        // Hanging moss under the leaves.
        for (BlockPos leaf : context.leaves()) {
            if (random.nextFloat() < trunkProbability && context.isAir(leaf.below())) {
                int length = 1 + random.nextInt(3);
                BlockPos.MutableBlockPos pos = leaf.below().mutable();
                for (int i = 0; i < length && context.isAir(pos); i++) {
                    boolean tip = i == length - 1 || !context.isAir(pos.below());
                    context.setBlock(pos.immutable(), ModBlocks.PALE_HANGING_MOSS.get().defaultBlockState().setValue(HangingMossBlock.TIP, tip));
                    if (tip) break;
                    pos.move(0, -1, 0);
                }
            }
        }

        // Pale moss on the ground around the trunk.
        BlockPos base = context.logs().stream().min((a, b) -> Integer.compare(a.getY(), b.getY())).orElseThrow();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 && Math.abs(dz) == 3 || random.nextFloat() >= groundProbability) continue;
                for (int dy = 1; dy >= -2; dy--) {
                    BlockPos ground = base.offset(dx, dy - 1, dz);
                    if (context.level().isStateAtPosition(ground, state -> state.is(BlockTags.MOSS_REPLACEABLE)) && context.isAir(ground.above())) {
                        context.setBlock(ground, ModBlocks.PALE_MOSS_BLOCK.get().defaultBlockState());
                        if (random.nextFloat() < 0.25F) {
                            context.setBlock(ground.above(), ModBlocks.PALE_MOSS_CARPET.get().defaultBlockState()
                                    .setValue(PaleMossCarpetBlock.BOTTOM, true));
                        }
                        break;
                    }
                }
            }
        }
    }
}
