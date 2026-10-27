package io.github.kestalf.futurebackport.block;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.entity.ghast.HappyGhast;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Dried ghast (1.21.6). Placed in water it slowly rehydrates through three stages (a stage every ~5 minutes) and then
 * turns into a ghastling; out of water it dries out again at the same rate.
 */
public class DriedGhastBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<DriedGhastBlock> CODEC = simpleCodec(DriedGhastBlock::new);
    public static final IntegerProperty HYDRATION = IntegerProperty.create("hydration", 0, 3);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int MAX_HYDRATION = 3;
    /** Delay between hydration stages; it starts with a random tick. */
    private static final int STAGE_DELAY = 5000;
    private static final VoxelShape SHAPE = box(3.0, 0.0, 3.0, 13.0, 10.0, 13.0);

    public DriedGhastBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HYDRATION, 0).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, water);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && state.getValue(WATERLOGGED)) {
            level.playSound(null, pos, ModSounds.event("block.dried_ghast.place_in_water").get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean changing = state.getValue(WATERLOGGED) || state.getValue(HYDRATION) > 0;
        if (changing && !level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, STAGE_DELAY);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int hydration = state.getValue(HYDRATION);
        if (state.getValue(WATERLOGGED)) {
            if (hydration < MAX_HYDRATION) {
                level.setBlock(pos, state.setValue(HYDRATION, hydration + 1), Block.UPDATE_ALL);
                level.playSound(null, pos, ModSounds.event("block.dried_ghast.transition").get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                hatch(state, level, pos);
            }
        } else if (hydration > 0) {
            level.setBlock(pos, state.setValue(HYDRATION, hydration - 1), Block.UPDATE_ALL);
        }
    }

    private static void hatch(BlockState state, ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        HappyGhast ghastling = ModEntities.HAPPY_GHAST.get().create(level);
        if (ghastling == null) return;
        ghastling.setBaby(true);
        ghastling.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, state.getValue(FACING).toYRot(), 0.0F);
        ghastling.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.BREEDING, null);
        ghastling.setBaby(true);
        level.addFreshEntity(ghastling);
        ghastling.playHatchEffects();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(WATERLOGGED)) {
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.HAPPY_VILLAGER, pos.getX() + random.nextDouble(), pos.getY() + 0.7,
                        pos.getZ() + random.nextDouble(), 0.0, 0.0, 0.0);
            }
            if (random.nextInt(400) == 0) {
                level.playLocalSound(pos, ModSounds.event("block.dried_ghast.ambient_water").get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
            }
        } else if (random.nextInt(400) == 0 && level.getBlockState(pos.below()).is(BlockTags.SOUL_SPEED_BLOCKS)) {
            level.playLocalSound(pos, ModSounds.event("block.dried_ghast.ambient").get(), SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HYDRATION, WATERLOGGED);
    }
}
