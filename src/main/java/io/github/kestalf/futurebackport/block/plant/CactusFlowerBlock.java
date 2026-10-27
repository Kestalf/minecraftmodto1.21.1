package io.github.kestalf.futurebackport.block.plant;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A flower that grows on top of cacti (and on anything with a sturdy center). */
public class CactusFlowerBlock extends BushBlock {
    public static final MapCodec<CactusFlowerBlock> CODEC = simpleCodec(CactusFlowerBlock::new);
    private static final VoxelShape SHAPE = box(1, 0, 1, 15, 12, 15);

    public CactusFlowerBlock(Properties properties) {
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
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.CACTUS) || state.is(Blocks.FARMLAND) || state.isFaceSturdy(level, pos, Direction.UP, SupportType.CENTER);
    }
}
