package io.github.kestalf.futurebackport.block.plant;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Pale moss carpet. Besides the carpet itself ({@code bottom=true}) it grows vine-like moss onto neighbouring walls:
 * {@code low} covers the lower part of the wall, {@code tall} the full block. A carpet with tall sides can carry a
 * second, carpet-less piece above it ({@code bottom=false}).
 */
public class PaleMossCarpetBlock extends Block implements BonemealableBlock {
    public static final MapCodec<PaleMossCarpetBlock> CODEC = simpleCodec(PaleMossCarpetBlock::new);
    public static final BooleanProperty BOTTOM = BooleanProperty.create("bottom");
    public static final Map<Direction, EnumProperty<WallSide>> SIDES = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH_WALL, Direction.EAST, BlockStateProperties.EAST_WALL,
            Direction.SOUTH, BlockStateProperties.SOUTH_WALL, Direction.WEST, BlockStateProperties.WEST_WALL);
    private static final VoxelShape CARPET = box(0, 0, 0, 16, 1, 16);
    private static final Map<Direction, VoxelShape> SIDE_SHAPES = Map.of(
            Direction.NORTH, box(0, 0, 0, 16, 16, 1), Direction.SOUTH, box(0, 0, 15, 16, 16, 16),
            Direction.WEST, box(0, 0, 0, 1, 16, 16), Direction.EAST, box(15, 0, 0, 16, 16, 16));

    public PaleMossCarpetBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(BOTTOM, true);
        for (EnumProperty<WallSide> side : SIDES.values()) state = state.setValue(side, WallSide.NONE);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = state.getValue(BOTTOM) ? CARPET : Shapes.empty();
        for (Map.Entry<Direction, EnumProperty<WallSide>> side : SIDES.entrySet()) {
            if (state.getValue(side.getValue()) != WallSide.NONE) shape = Shapes.or(shape, SIDE_SHAPES.get(side.getKey()));
        }
        return shape.isEmpty() ? CARPET : shape;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(BOTTOM) ? CARPET : Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(BOTTOM)) return !level.isEmptyBlock(pos.below());
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) && below.getValue(BOTTOM) && hasAnySide(state);
    }

    private static boolean hasAnySide(BlockState state) {
        return SIDES.values().stream().anyMatch(p -> state.getValue(p) != WallSide.NONE);
    }

    private static boolean canAttachTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos wall = pos.relative(direction);
        return level.getBlockState(wall).isFaceSturdy(level, wall, direction.getOpposite());
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Map.Entry<Direction, EnumProperty<WallSide>> side : SIDES.entrySet()) {
            if (canAttachTo(context.getLevel(), context.getClickedPos(), side.getKey())) {
                state = state.setValue(side.getValue(), WallSide.LOW);
            }
        }
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && level.getRandom().nextBoolean()) grow(level, pos, state);
    }

    /** Makes the low sides tall and places a top piece above where the walls continue. */
    private boolean grow(Level level, BlockPos pos, BlockState state) {
        BlockPos above = pos.above();
        if (!level.isEmptyBlock(above)) return false;
        BlockState grown = state;
        BlockState top = defaultBlockState().setValue(BOTTOM, false);
        boolean any = false;
        for (Map.Entry<Direction, EnumProperty<WallSide>> side : SIDES.entrySet()) {
            if (state.getValue(side.getValue()) == WallSide.NONE) continue;
            grown = grown.setValue(side.getValue(), WallSide.TALL);
            if (canAttachTo(level, above, side.getKey())) {
                top = top.setValue(side.getValue(), WallSide.LOW);
                any = true;
            }
        }
        level.setBlock(pos, grown, Block.UPDATE_ALL);
        if (any) level.setBlock(above, top, Block.UPDATE_ALL);
        return true;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis().isHorizontal()) {
            EnumProperty<WallSide> side = SIDES.get(direction);
            if (state.getValue(side) != WallSide.NONE && !canAttachTo(level, pos, direction)) state = state.setValue(side, WallSide.NONE);
        }
        return state.canSurvive(level, pos) ? state : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !state.getValue(BOTTOM) && !context.getItemInHand().is(asItem()) || super.canBeReplaced(state, context);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(BOTTOM) && hasAnySide(state) && level.isEmptyBlock(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(level, pos, state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState result = state;
        for (Map.Entry<Direction, EnumProperty<WallSide>> side : SIDES.entrySet()) {
            result = result.setValue(SIDES.get(rotation.rotate(side.getKey())), state.getValue(side.getValue()));
        }
        return result;
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState result = state;
        for (Map.Entry<Direction, EnumProperty<WallSide>> side : SIDES.entrySet()) {
            result = result.setValue(SIDES.get(mirror.mirror(side.getKey())), state.getValue(side.getValue()));
        }
        return result;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BOTTOM, BlockStateProperties.NORTH_WALL, BlockStateProperties.EAST_WALL, BlockStateProperties.SOUTH_WALL,
                BlockStateProperties.WEST_WALL);
    }
}
