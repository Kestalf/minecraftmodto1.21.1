package io.github.kestalf.futurebackport.block.plant;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * A small plant that spreads a copy of itself to a neighbouring block when bone meal is used on it
 * (bush, firefly bush, red shrub).
 */
public class SpreadingBushBlock extends BushBlock implements BonemealableBlock {
    public static final MapCodec<SpreadingBushBlock> CODEC = simpleCodec(SpreadingBushBlock::new);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 13, 16);

    public SpreadingBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return !findSpreadTargets(level, pos, state).isEmpty();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        List<BlockPos> targets = findSpreadTargets(level, pos, state);
        if (!targets.isEmpty()) level.setBlockAndUpdate(targets.get(random.nextInt(targets.size())), defaultBlockState());
    }

    protected List<BlockPos> findSpreadTargets(LevelReader level, BlockPos pos, BlockState state) {
        List<BlockPos> result = new ArrayList<>();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos target = pos.relative(direction);
            if (level.isEmptyBlock(target) && state.canSurvive(level, target)) result.add(target);
        }
        return result;
    }
}
