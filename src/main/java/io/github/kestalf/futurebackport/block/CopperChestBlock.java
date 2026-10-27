package io.github.kestalf.futurebackport.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.kestalf.futurebackport.block.entity.CopperChestBlockEntity;
import io.github.kestalf.futurebackport.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * Copper chest: a chest that oxidizes and that copper golems empty. Copper chests of any stage connect to each
 * other; the half that changed last (placed, oxidized, waxed or scraped) decides the block of both halves. Changing
 * between copper chest blocks keeps the block entity, so the contents survive oxidation, waxing and scraping.
 */
public class CopperChestBlock extends ChestBlock implements WeatheringCopper {
    public static final MapCodec<CopperChestBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            WeatherState.CODEC.fieldOf("weathering_state").forGetter(CopperChestBlock::getAge),
            com.mojang.serialization.Codec.BOOL.fieldOf("waxed").forGetter(CopperChestBlock::isWaxed),
            propertiesCodec()
    ).apply(instance, CopperChestBlock::new));
    private final WeatherState weatherState;
    private final boolean waxed;

    public CopperChestBlock(WeatherState weatherState, boolean waxed, Properties properties) {
        super(properties, ModBlockEntities.COPPER_CHEST::get);
        this.weatherState = weatherState;
        this.waxed = waxed;
    }

    @Override
    public MapCodec<? extends ChestBlock> codec() {
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
        return new CopperChestBlockEntity(pos, state);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        changeOverTime(state, level, pos, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !waxed && WeatheringCopper.getNext(state.getBlock()).isPresent();
    }

    private static boolean isCopperChest(BlockState state) {
        return state.getBlock() instanceof CopperChestBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        ChestType type = ChestType.SINGLE;
        Direction facing = context.getHorizontalDirection().getOpposite();
        boolean sneaking = context.isSecondaryUseActive();
        Direction clickedFace = context.getClickedFace();
        if (clickedFace.getAxis().isHorizontal() && sneaking) {
            Direction partnerFacing = candidatePartnerFacing(context, clickedFace.getOpposite());
            if (partnerFacing != null && partnerFacing.getAxis() != clickedFace.getAxis()) {
                facing = partnerFacing;
                type = partnerFacing.getCounterClockWise() == clickedFace.getOpposite() ? ChestType.RIGHT : ChestType.LEFT;
            }
        }
        if (type == ChestType.SINGLE && !sneaking) {
            if (facing == candidatePartnerFacing(context, facing.getClockWise())) {
                type = ChestType.LEFT;
            } else if (facing == candidatePartnerFacing(context, facing.getCounterClockWise())) {
                type = ChestType.RIGHT;
            }
        }
        return defaultBlockState().setValue(FACING, facing).setValue(TYPE, type)
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    @Nullable
    private static Direction candidatePartnerFacing(BlockPlaceContext context, Direction direction) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos().relative(direction));
        return isCopperChest(state) && state.getValue(TYPE) == ChestType.SINGLE ? state.getValue(FACING) : null;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos,
                                     BlockPos facingPos) {
        if (isCopperChest(facingState) && facing.getAxis().isHorizontal()) {
            if (state.getValue(WATERLOGGED)) level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            ChestType partnerType = facingState.getValue(TYPE);
            if (state.getValue(TYPE) == ChestType.SINGLE && partnerType != ChestType.SINGLE && state.getValue(FACING) == facingState.getValue(FACING)
                    && getConnectedDirection(facingState) == facing.getOpposite()) {
                return state.setValue(TYPE, partnerType.getOpposite());
            }
            return state;
        }
        return super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || state.getValue(TYPE) == ChestType.SINGLE) return;
        BlockPos partnerPos = pos.relative(getConnectedDirection(state));
        BlockState partner = level.getBlockState(partnerPos);
        if (isCopperChest(partner) && partner.getBlock() != this && partner.getValue(FACING) == state.getValue(FACING)
                && partner.getValue(TYPE) == state.getValue(TYPE).getOpposite()) {
            level.setBlock(partnerPos, defaultBlockState().setValue(FACING, partner.getValue(FACING)).setValue(TYPE, partner.getValue(TYPE))
                    .setValue(WATERLOGGED, partner.getValue(WATERLOGGED)), UPDATE_ALL);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Oxidation, waxing and scraping swap the block: keep the block entity and its items.
        if (isCopperChest(newState)) return;
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
