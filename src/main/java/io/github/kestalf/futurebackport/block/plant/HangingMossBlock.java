package io.github.kestalf.futurebackport.block.plant;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Pale hanging moss: hangs from blocks and leaves, bone meal makes it one block longer. */
public class HangingMossBlock extends Block implements BonemealableBlock {
    public static final MapCodec<HangingMossBlock> CODEC = simpleCodec(HangingMossBlock::new);
    public static final BooleanProperty TIP = BooleanProperty.create("tip");
    private static final VoxelShape TIP_SHAPE = box(1, 2, 1, 15, 16, 15);
    private static final VoxelShape BASE_SHAPE = box(1, 0, 1, 15, 16, 15);

    public HangingMossBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIP, true));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TIP) ? TIP_SHAPE : BASE_SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        return aboveState.is(this) || aboveState.is(BlockTags.LEAVES) || aboveState.isFaceSturdy(level, above, Direction.DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(TIP, !context.getLevel().getBlockState(context.getClickedPos().below()).is(this));
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!state.canSurvive(level, pos)) {
            level.scheduleTick(pos, this, 1);
        }
        return direction == Direction.DOWN ? state.setValue(TIP, !neighborState.is(this)) : state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(500) == 0) {
            BlockState above = level.getBlockState(pos.above());
            if (above.is(BlockTags.LEAVES) || above.is(BlockTags.LOGS)) {
                level.playLocalSound(pos, ModSounds.event("block.pale_hanging_moss.idle").get(), SoundSource.AMBIENT, 1.0F, 1.0F, false);
            }
        }
    }

    private BlockPos findTip(LevelReader level, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = pos.mutable();
        while (level.getBlockState(cursor.below()).is(this)) cursor.move(Direction.DOWN);
        return cursor.immutable();
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        BlockPos below = findTip(level, pos).below();
        return !level.isOutsideBuildHeight(below) && level.getBlockState(below).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos tip = findTip(level, pos);
        level.setBlock(tip, level.getBlockState(tip).setValue(TIP, false), Block.UPDATE_CLIENTS);
        level.setBlock(tip.below(), defaultBlockState().setValue(TIP, true), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP);
    }
}
