package io.github.kestalf.futurebackport.block;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.block.entity.PotentSulfurBlockEntity;
import io.github.kestalf.futurebackport.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.Nullable;

/**
 * Potent sulfur: under shallow still water it gives off noxious gas; with a magma block below it is a geyser that
 * erupts from time to time, with lava below it erupts all the time. See {@link PotentSulfurBlockEntity}.
 */
public class PotentSulfurBlock extends BaseEntityBlock {
    public static final MapCodec<PotentSulfurBlock> CODEC = simpleCodec(PotentSulfurBlock::new);
    public static final EnumProperty<State> STATE = EnumProperty.create("potent_sulfur_state", State.class);

    public PotentSulfurBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        registerDefaultState(defaultBlockState().setValue(STATE, State.DRY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotentSulfurBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.POTENT_SULFUR.get(),
                level.isClientSide ? PotentSulfurBlockEntity::clientTick : PotentSulfurBlockEntity::serverTick);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return withEnvironment(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        return direction.getAxis() == Direction.Axis.Y ? withEnvironment(state, level, pos) : state;
    }

    /** Dry without water above; wet over anything else; a geyser (dormant/erupting) over magma; continuous over lava. */
    public static BlockState withEnvironment(BlockState state, LevelAccessor level, BlockPos pos) {
        if (!level.getFluidState(pos.above()).is(FluidTags.WATER)) return state.setValue(STATE, State.DRY);
        BlockState below = level.getBlockState(pos.below());
        if (below.getFluidState().is(FluidTags.LAVA)) return state.setValue(STATE, State.CONTINUOUS);
        if (below.is(Blocks.MAGMA_BLOCK)) {
            State current = state.getValue(STATE);
            return state.setValue(STATE, current == State.ERUPTING ? State.ERUPTING : State.DORMANT);
        }
        return state.setValue(STATE, State.WET);
    }

    public enum State implements StringRepresentable {
        DRY("dry"),
        WET("wet"),
        DORMANT("dormant"),
        ERUPTING("erupting"),
        CONTINUOUS("continuous");

        private final String name;

        State(String name) {
            this.name = name;
        }

        public boolean isGeyser() {
            return this == ERUPTING || this == CONTINUOUS;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
