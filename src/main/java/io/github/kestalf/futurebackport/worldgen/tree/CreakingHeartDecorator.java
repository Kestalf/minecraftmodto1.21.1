package io.github.kestalf.futurebackport.worldgen.tree;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.block.CreakingHeartBlock;
import io.github.kestalf.futurebackport.block.CreakingHeartState;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.worldgen.ModWorldgen;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;

import java.util.ArrayList;
import java.util.List;

/** Hides a natural creaking heart in a trunk log that is fully enclosed by logs. */
public class CreakingHeartDecorator extends TreeDecorator {
    public static final MapCodec<CreakingHeartDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability")
            .xmap(CreakingHeartDecorator::new, d -> d.probability);
    private final float probability;

    public CreakingHeartDecorator(float probability) {
        this.probability = probability;
    }

    @Override
    protected TreeDecoratorType<?> type() {
        return ModWorldgen.CREAKING_HEART.get();
    }

    @Override
    public void place(Context context) {
        List<BlockPos> logs = new ArrayList<>(context.logs());
        if (logs.isEmpty() || context.random().nextFloat() >= probability) return;
        Util.shuffle(logs, context.random());
        logs.stream().filter(pos -> {
            for (Direction direction : Direction.values()) {
                if (!context.level().isStateAtPosition(pos.relative(direction), state -> state.is(BlockTags.LOGS))) return false;
            }
            return true;
        }).findFirst().ifPresent(pos -> context.setBlock(pos, ModBlocks.CREAKING_HEART.get().defaultBlockState()
                .setValue(CreakingHeartBlock.STATE, CreakingHeartState.DORMANT).setValue(CreakingHeartBlock.NATURAL, true)));
    }
}
