package io.github.kestalf.futurebackport.block.plant;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Shelf mushroom: grows on the side of blocks (mostly poplar logs), comes in two sizes and is bouncy. */
public class ShelfMushroomBlock extends HorizontalDirectionalBlock implements BonemealableBlock {
    public static final MapCodec<ShelfMushroomBlock> CODEC = simpleCodec(ShelfMushroomBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_1;
    private static final Map<Direction, VoxelShape> SMALL = shapes(3, 8, 9, 13, 11, 16);
    private static final Map<Direction, VoxelShape> BIG = shapes(1, 6, 6, 15, 11, 16);

    public ShelfMushroomBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(AGE, 0));
    }

    /** Builds the shape for each facing from the north-facing box (the support is on the south side). */
    private static Map<Direction, VoxelShape> shapes(double x1, double y1, double z1, double x2, double y2, double z2) {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, box(x1, y1, z1, x2, y2, z2));
        map.put(Direction.SOUTH, box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1));
        map.put(Direction.WEST, box(z1, y1, 16 - x2, z2, y2, 16 - x1));
        map.put(Direction.EAST, box(16 - z2, y1, x1, 16 - z1, y2, x2));
        return map;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (state.getValue(AGE) == 0 ? SMALL : BIG).get(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis().isHorizontal()) {
                BlockState state = defaultBlockState().setValue(FACING, direction.getOpposite());
                if (state.canSurvive(context.getLevel(), context.getClickedPos())) return state;
            }
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance * 0.5F);
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
            return;
        }
        Vec3 movement = entity.getDeltaMovement();
        if (movement.y < 0.0) {
            double factor = entity instanceof LivingEntity ? 1.0 : 0.8;
            entity.setDeltaMovement(movement.x, -movement.y * 0.66 * factor, movement.z);
            if (movement.y < -0.2 && entity.level() instanceof Level world && !world.isClientSide) {
                world.playSound(null, entity.blockPosition(), ModSounds.event("block.shelf_mushroom.bounce").get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return state.getValue(AGE) == 0;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.setBlock(pos, state.setValue(AGE, 1), Block.UPDATE_ALL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, AGE);
    }
}
