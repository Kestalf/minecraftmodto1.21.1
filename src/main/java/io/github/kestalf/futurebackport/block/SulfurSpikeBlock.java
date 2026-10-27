package io.github.kestalf.futurebackport.block;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Sulfur spike: stalactites and stalagmites shaped like pointed dripstone. Stalactites under a sulfur block slowly
 * grow (up to three blocks, or grow a stalagmite on the ground below) and fall when they lose their support;
 * stalagmites do not add fall damage.
 */
public class SulfurSpikeBlock extends Block implements Fallable, SimpleWaterloggedBlock {
    public static final MapCodec<SulfurSpikeBlock> CODEC = simpleCodec(SulfurSpikeBlock::new);
    public static final DirectionProperty TIP_DIRECTION = BlockStateProperties.VERTICAL_DIRECTION;
    public static final EnumProperty<DripstoneThickness> THICKNESS = BlockStateProperties.DRIPSTONE_THICKNESS;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final float GROWTH_CHANCE = 64.0F / 5625.0F;
    private static final int MAX_STALACTITE_LENGTH = 3;
    private static final int MAX_GROWTH_DISTANCE = 11;
    private static final VoxelShape TIP_MERGE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape TIP_UP = Block.box(5.0, 0.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape TIP_DOWN = Block.box(5.0, 5.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape FRUSTUM = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape MIDDLE = Block.box(3.0, 0.0, 3.0, 13.0, 16.0, 13.0);
    private static final VoxelShape BASE = Block.box(2.0, 0.0, 2.0, 14.0, 16.0, 14.0);

    public SulfurSpikeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(TIP_DIRECTION, Direction.UP).setValue(THICKNESS, DripstoneThickness.TIP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIP_DIRECTION, THICKNESS, WATERLOGGED);
    }

    private boolean isSpike(BlockState state, Direction direction) {
        return state.is(this) && state.getValue(TIP_DIRECTION) == direction;
    }

    // ------------------------------------------------------------------ placement and shape

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction tip = state.getValue(TIP_DIRECTION);
        BlockPos supportPos = pos.relative(tip.getOpposite());
        BlockState support = level.getBlockState(supportPos);
        return support.isFaceSturdy(level, supportPos, tip) || isSpike(support, tip);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        LevelAccessor level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction wanted = context.getNearestLookingVerticalDirection().getOpposite();
        Direction tip = findTipDirection(level, pos, wanted);
        if (tip == null) return null;
        boolean merge = !context.isSecondaryUseActive();
        DripstoneThickness thickness = calculateThickness(level, pos, tip, merge);
        return defaultBlockState().setValue(TIP_DIRECTION, tip).setValue(THICKNESS, thickness)
                .setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
    }

    @Nullable
    private Direction findTipDirection(LevelReader level, BlockPos pos, Direction wanted) {
        BlockState candidate = defaultBlockState().setValue(TIP_DIRECTION, wanted);
        if (canSurvive(candidate, level, pos)) return wanted;
        candidate = defaultBlockState().setValue(TIP_DIRECTION, wanted.getOpposite());
        return canSurvive(candidate, level, pos) ? wanted.getOpposite() : null;
    }

    private DripstoneThickness calculateThickness(LevelReader level, BlockPos pos, Direction tip, boolean merge) {
        BlockState ahead = level.getBlockState(pos.relative(tip));
        if (isSpike(ahead, tip.getOpposite())) {
            return merge || ahead.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE ? DripstoneThickness.TIP_MERGE : DripstoneThickness.TIP;
        }
        if (!isSpike(ahead, tip)) return DripstoneThickness.TIP;
        DripstoneThickness aheadThickness = ahead.getValue(THICKNESS);
        if (aheadThickness == DripstoneThickness.TIP || aheadThickness == DripstoneThickness.TIP_MERGE) return DripstoneThickness.FRUSTUM;
        BlockState behind = level.getBlockState(pos.relative(tip.getOpposite()));
        return isSpike(behind, tip) ? DripstoneThickness.MIDDLE : DripstoneThickness.BASE;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        if (direction != Direction.UP && direction != Direction.DOWN) return state;
        Direction tip = state.getValue(TIP_DIRECTION);
        if (tip == Direction.DOWN && level.getBlockTicks().hasScheduledTick(pos, this)) return state;
        if (direction == tip.getOpposite() && !canSurvive(state, level, pos)) {
            // Stalactites fall (scheduled), stalagmites break.
            level.scheduleTick(pos, this, tip == Direction.DOWN ? 2 : 1);
            return state;
        }
        boolean merged = state.getValue(THICKNESS) == DripstoneThickness.TIP_MERGE;
        return state.setValue(THICKNESS, calculateThickness(level, pos, tip, merged));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = switch (state.getValue(THICKNESS)) {
            case TIP_MERGE -> TIP_MERGE;
            case TIP -> state.getValue(TIP_DIRECTION) == Direction.DOWN ? TIP_DOWN : TIP_UP;
            case FRUSTUM -> FRUSTUM;
            case MIDDLE -> MIDDLE;
            case BASE -> BASE;
        };
        return shape;
    }

    @Override
    protected boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    // ------------------------------------------------------------------ falling, growing, breaking

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (canSurvive(state, level, pos)) return;
        if (state.getValue(TIP_DIRECTION) == Direction.UP) {
            level.destroyBlock(pos, true);
            return;
        }
        // Drop the whole unsupported stalactite, tip first.
        BlockPos.MutableBlockPos cursor = pos.mutable();
        BlockState current = state;
        while (isSpike(current, Direction.DOWN)) {
            FallingBlockEntity falling = FallingBlockEntity.fall(level, cursor, current.setValue(WATERLOGGED, false));
            int length = Math.max(1 + pos.getY() - cursor.getY(), 6);
            falling.setHurtsEntities(length, 40);
            cursor.move(Direction.DOWN);
            current = level.getBlockState(cursor);
        }
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity falling) {
        if (!falling.isSilent()) {
            level.playSound(null, pos, io.github.kestalf.futurebackport.registry.ModSounds.event("block.sulfur_spike.land").get(),
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    public DamageSource getFallDamageSource(Entity entity) {
        return entity.damageSources().fallingStalactite(entity);
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (!level.isClientSide && projectile instanceof ThrownTrident && projectile.mayInteract(level, hit.getBlockPos())) {
            level.destroyBlock(hit.getBlockPos(), true);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(TIP_DIRECTION) == Direction.DOWN && state.getValue(THICKNESS) == DripstoneThickness.TIP
                && !state.getValue(WATERLOGGED);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextFloat() >= GROWTH_CHANCE) return;
        // Find the root of the stalactite; it only grows under sulfur.
        BlockPos root = pos;
        int length = 1;
        while (isSpike(level.getBlockState(root.above()), Direction.DOWN)) {
            root = root.above();
            length++;
        }
        if (!level.getBlockState(root.above()).is(ModBlocks.SULFUR.base().get())) return;
        if (length < MAX_STALACTITE_LENGTH && random.nextBoolean()) {
            BlockPos below = pos.below();
            if (level.getBlockState(below).isAir()) grow(level, below, Direction.DOWN);
            return;
        }
        if (length >= MAX_STALACTITE_LENGTH) return;
        BlockPos.MutableBlockPos cursor = pos.below().mutable();
        for (int i = 0; i < MAX_GROWTH_DISTANCE; i++) {
            BlockState below = level.getBlockState(cursor);
            if (isSpike(below, Direction.UP)) {
                if (below.getValue(THICKNESS) == DripstoneThickness.TIP && level.getBlockState(cursor.above()).isAir()) {
                    grow(level, cursor.above().immutable(), Direction.UP);
                }
                return;
            }
            if (!below.isAir()) {
                if (below.isFaceSturdy(level, cursor, Direction.UP)) grow(level, cursor.above().immutable(), Direction.UP);
                return;
            }
            cursor.move(Direction.DOWN);
        }
    }

    private void grow(ServerLevel level, BlockPos pos, Direction tip) {
        if (!level.getBlockState(pos).isAir()) return;
        BlockState state = defaultBlockState().setValue(TIP_DIRECTION, tip);
        level.setBlock(pos, state.setValue(THICKNESS, calculateThickness(level, pos, tip, true)), Block.UPDATE_ALL);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        // Unlike pointed dripstone, sulfur stalagmites do not make falls hurt more.
        super.fallOn(level, state, pos, entity, fallDistance);
    }
}
