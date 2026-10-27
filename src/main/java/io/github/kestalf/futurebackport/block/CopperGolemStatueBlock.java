package io.github.kestalf.futurebackport.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kestalf.futurebackport.block.entity.CopperGolemStatueBlockEntity;
import io.github.kestalf.futurebackport.entity.golem.CopperGolem;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A copper golem frozen in one of four poses. Using it with an empty hand changes the pose; scraping an unwaxed,
 * unoxidized statue with an axe brings the golem back to life.
 */
public class CopperGolemStatueBlock extends BaseEntityBlock implements SimpleWaterloggedBlock, WeatheringCopper {
    public static final MapCodec<CopperGolemStatueBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperGolemStatueBlock::getAge),
            Codec.BOOL.fieldOf("waxed").forGetter(CopperGolemStatueBlock::isWaxed),
            propertiesCodec()
    ).apply(instance, CopperGolemStatueBlock::new));
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<CopperGolemPose> POSE = EnumProperty.create("copper_golem_pose", CopperGolemPose.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.box(3.0, 0.0, 3.0, 13.0, 14.0, 13.0);
    private final WeatherState weatherState;
    private final boolean waxed;

    public CopperGolemStatueBlock(WeatherState weatherState, boolean waxed, Properties properties) {
        super(properties);
        this.weatherState = weatherState;
        this.waxed = waxed;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POSE, CopperGolemPose.STANDING)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public WeatherState getAge() {
        return weatherState;
    }

    public boolean isWaxed() {
        return waxed;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperGolemStatueBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POSE, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        changeOverTime(state, level, pos, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !waxed && WeatheringCopper.getNext(state.getBlock()).isPresent();
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(ItemTags.AXES) && !waxed && weatherState == WeatherState.UNAFFECTED) {
            if (level instanceof ServerLevel server) {
                reanimate(server, pos, state);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // Other items (axes scraping oxidation or wax, honeycomb) do their own thing.
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.setBlock(pos, state.setValue(POSE, state.getValue(POSE).next()), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.event("block.copper_golem_statue.hit").get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void reanimate(ServerLevel level, BlockPos pos, BlockState state) {
        CopperGolem golem = ModEntities.COPPER_GOLEM.get().create(level);
        if (golem == null) return;
        golem.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, state.getValue(FACING).toYRot(), 0.0F);
        golem.setYHeadRot(golem.getYRot());
        golem.setYBodyRot(golem.getYRot());
        if (level.getBlockEntity(pos) instanceof CopperGolemStatueBlockEntity statue && statue.getCustomName() != null) {
            golem.setCustomName(statue.getCustomName());
        }
        golem.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CONVERSION, null);
        level.removeBlock(pos, false);
        level.addFreshEntity(golem);
        golem.playSound(ModSounds.event("entity.copper_golem.spawn").get(), 1.0F, 1.0F);
        level.gameEvent(golem, GameEvent.ENTITY_PLACE, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Oxidation, waxing and scraping swap the block: keep the name.
        if (newState.getBlock() instanceof CopperGolemStatueBlock) return;
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(POSE).ordinal() + 1;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
