package io.github.kestalf.futurebackport.block;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.block.entity.CreakingHeartBlockEntity;
import io.github.kestalf.futurebackport.registry.ModBlockEntities;
import io.github.kestalf.futurebackport.registry.ModSounds;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Creaking heart: placed between two pale oak logs of the same axis it wakes up at night and spawns a creaking that
 * it protects; breaking the heart kills that creaking.
 */
public class CreakingHeartBlock extends BaseEntityBlock {
    public static final MapCodec<CreakingHeartBlock> CODEC = simpleCodec(CreakingHeartBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    public static final EnumProperty<CreakingHeartState> STATE = EnumProperty.create("creaking_heart_state", CreakingHeartState.class);
    public static final BooleanProperty NATURAL = BooleanProperty.create("natural");

    public CreakingHeartBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(AXIS, Direction.Axis.Y).setValue(STATE, CreakingHeartState.UPROOTED)
                .setValue(NATURAL, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreakingHeartBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlockEntities.CREAKING_HEART.get(), CreakingHeartBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public static boolean isNaturalNight(Level level) {
        return level.dimensionType().natural() && level.isNight();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (isNaturalNight(level) && state.getValue(STATE) != CreakingHeartState.UPROOTED && random.nextInt(16) == 0
                && isSurroundedByLogs(level, pos)) {
            level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), ModSounds.event("block.creaking_heart.idle").get(),
                    SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
    }

    private static BlockState updateState(BlockState state, Level level, BlockPos pos) {
        boolean logs = hasRequiredLogs(state, level, pos);
        boolean uprooted = state.getValue(STATE) == CreakingHeartState.UPROOTED;
        if (logs && uprooted) return state.setValue(STATE, isNaturalNight(level) ? CreakingHeartState.AWAKE : CreakingHeartState.DORMANT);
        return state;
    }

    public static boolean hasRequiredLogs(BlockState state, LevelAccessor level, BlockPos pos) {
        Direction.Axis axis = state.getValue(AXIS);
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != axis) continue;
            BlockState neighbor = level.getBlockState(pos.relative(direction));
            if (!neighbor.is(ModTags.Blocks.PALE_OAK_LOGS) || !neighbor.hasProperty(AXIS) || neighbor.getValue(AXIS) != axis) return false;
        }
        return true;
    }

    private static boolean isSurroundedByLogs(LevelAccessor level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (!level.getBlockState(pos.relative(direction)).is(BlockTags.LOGS)) return false;
        }
        return true;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateState(defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis()), context.getLevel(),
                context.getClickedPos());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return RotatedPillarBlock.rotatePillar(state, rotation);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, STATE, NATURAL);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity heart) {
            heart.removeProtector(null);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity heart) {
            heart.removeProtector(player.damageSources().playerAttack(player));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public int getExpDrop(BlockState state, LevelAccessor level, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity breaker,
                          ItemStack tool) {
        return state.getValue(NATURAL) ? UniformInt.of(20, 24).sample(level.getRandom()) : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CreakingHeartBlockEntity heart ? heart.getAnalogOutputSignal() : 0;
    }

    @Override
    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
}
