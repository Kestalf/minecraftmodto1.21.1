package io.github.kestalf.futurebackport.block;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.block.entity.ShelfBlockEntity;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Shelf: shows three item stacks; using a slot swaps it with the item in hand. Powered shelves join with up to two
 * powered neighbours facing the same way (left first) and swap all their items with the rightmost hotbar slots.
 */
public class ShelfBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<ShelfBlock> CODEC = simpleCodec(ShelfBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<SideChain> SIDE_CHAIN = EnumProperty.create("side_chain", SideChain.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int MAX_CHAIN = 3;
    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES.put(facing, Shapes.or(rotated(facing, 0, 0, 13, 16, 16, 16), rotated(facing, 0, 0, 11, 16, 4, 13),
                    rotated(facing, 0, 12, 11, 16, 16, 13)));
        }
    }

    /** A box given for a north-facing shelf (front towards -z), turned to face {@code facing}. */
    private static VoxelShape rotated(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
        return switch (facing) {
            case SOUTH -> Block.box(16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1);
            case WEST -> Block.box(z1, y1, 16 - x2, z2, y2, 16 - x1);
            case EAST -> Block.box(16 - z2, y1, x1, 16 - z1, y2, x2);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }

    public ShelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(SIDE_CHAIN, SideChain.UNCONNECTED).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShelfBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SIDE_CHAIN, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()))
                .setValue(WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER);
    }

    /** As seen by someone looking at the front. */
    private static Direction left(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    // ------------------------------------------------------------------ redstone and chains

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
            level.playSound(null, pos, ModSounds.event(powered ? "block.shelf.activate" : "block.shelf.deactivate").get(), SoundSource.BLOCKS,
                    1.0F, 1.0F);
            level.gameEvent(null, powered ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pos);
            updateChains(level, pos);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (level.isClientSide || oldState.is(this)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            // Placed without a placement context (commands, structures): pick up the redstone signal now.
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_ALL);
        }
        updateChains(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) {
                Containers.dropContents(level, pos, shelf);
                level.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, level, pos, newState, movedByPiston);
            Direction left = left(state);
            reconnect(level, pos.relative(left), state.getValue(FACING));
            reconnect(level, pos.relative(left.getOpposite()), state.getValue(FACING));
        }
    }

    private static boolean isChainable(BlockState state, Direction facing) {
        return state.getBlock() instanceof ShelfBlock && state.getValue(FACING) == facing && state.getValue(POWERED);
    }

    /** Recomputes the chains around a shelf whose power changed or that was placed. */
    private static void updateChains(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ShelfBlock)) return;
        Direction facing = state.getValue(FACING);
        if (state.getValue(POWERED)) {
            reconnect(level, pos, facing);
        } else {
            if (state.getValue(SIDE_CHAIN) != SideChain.UNCONNECTED) {
                level.setBlock(pos, state.setValue(SIDE_CHAIN, SideChain.UNCONNECTED), Block.UPDATE_ALL);
            }
            Direction left = left(state);
            reconnect(level, pos.relative(left), facing);
            reconnect(level, pos.relative(left.getOpposite()), facing);
        }
    }

    /** Splits the row of powered shelves through {@code pos} into chains of up to three, starting from the left. */
    private static void reconnect(Level level, BlockPos pos, Direction facing) {
        if (!isChainable(level.getBlockState(pos), facing)) return;
        Direction left = facing.getClockWise();
        BlockPos start = pos;
        for (int i = 0; i < 64 && isChainable(level.getBlockState(start.relative(left)), facing); i++) start = start.relative(left);
        List<BlockPos> row = new ArrayList<>();
        for (BlockPos p = start; isChainable(level.getBlockState(p), facing) && row.size() < 64; p = p.relative(left.getOpposite())) row.add(p);
        for (int i = 0; i < row.size(); i++) {
            int groupStart = i - i % MAX_CHAIN;
            int groupSize = Math.min(MAX_CHAIN, row.size() - groupStart);
            int index = i - groupStart;
            SideChain chain = groupSize == 1 ? SideChain.UNCONNECTED
                    : index == 0 ? SideChain.LEFT : index == groupSize - 1 ? SideChain.RIGHT : SideChain.CENTER;
            BlockState current = level.getBlockState(row.get(i));
            if (current.getValue(SIDE_CHAIN) != chain) level.setBlock(row.get(i), current.setValue(SIDE_CHAIN, chain), Block.UPDATE_ALL);
        }
    }

    /** The shelves joined with this one, from left to right. */
    private static List<BlockPos> chain(Level level, BlockPos pos, BlockState state) {
        Direction left = left(state);
        BlockPos start = pos;
        BlockState current = state;
        for (int i = 0; i < MAX_CHAIN - 1 && (current.getValue(SIDE_CHAIN) == SideChain.CENTER || current.getValue(SIDE_CHAIN) == SideChain.RIGHT); i++) {
            start = start.relative(left);
            current = level.getBlockState(start);
            if (!(current.getBlock() instanceof ShelfBlock)) return List.of(pos);
        }
        List<BlockPos> result = new ArrayList<>();
        BlockPos p = start;
        for (int i = 0; i < MAX_CHAIN; i++) {
            BlockState s = level.getBlockState(p);
            if (!(s.getBlock() instanceof ShelfBlock)) break;
            result.add(p);
            SideChain chain = s.getValue(SIDE_CHAIN);
            if (chain == SideChain.UNCONNECTED || chain == SideChain.RIGHT) break;
            p = p.relative(left.getOpposite());
        }
        return result.isEmpty() ? List.of(pos) : result;
    }

    // ------------------------------------------------------------------ using

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (hit.getDirection() != state.getValue(FACING) || !(level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(POWERED)) {
            if (!level.isClientSide) swapWithHotbar(level, pos, state, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        int slot = slotAt(state, pos, hit.getLocation());
        ItemStack stored = shelf.getItem(slot);
        if (stored.isEmpty() && stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            shelf.setItem(slot, stack.copyAndClear());
            player.setItemInHand(hand, stored);
            playSound(level, pos, stored.isEmpty() ? "block.shelf.place_item" : "block.shelf.single_swap");
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 0, 1 or 2 for the left, middle or right third of the front. */
    private static int slotAt(BlockState state, BlockPos pos, Vec3 hit) {
        Direction left = left(state);
        Vec3 local = hit.subtract(Vec3.atCenterOf(pos));
        double along = local.x * left.getStepX() + local.z * left.getStepZ();
        return along > 1.0 / 6.0 ? 0 : along < -1.0 / 6.0 ? 2 : 1;
    }

    private static void swapWithHotbar(Level level, BlockPos pos, BlockState state, Player player) {
        List<BlockPos> shelves = chain(level, pos, state);
        Inventory inventory = player.getInventory();
        int first = Inventory.getSelectionSize() - shelves.size() * ShelfBlockEntity.SLOTS;
        for (int i = 0; i < shelves.size(); i++) {
            if (!(level.getBlockEntity(shelves.get(i)) instanceof ShelfBlockEntity shelf)) continue;
            for (int slot = 0; slot < ShelfBlockEntity.SLOTS; slot++) {
                int hotbar = first + i * ShelfBlockEntity.SLOTS + slot;
                ItemStack fromShelf = shelf.getItem(slot);
                shelf.setItem(slot, inventory.getItem(hotbar));
                inventory.setItem(hotbar, fromShelf);
            }
            level.updateNeighbourForOutputSignal(shelves.get(i), state.getBlock());
        }
        playSound(level, pos, "block.shelf.multi_swap");
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
    }

    private static void playSound(Level level, BlockPos pos, String name) {
        SoundEvent sound = ModSounds.event(name).get();
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ misc

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf ? shelf.getAnalogOutputSignal() : 0;
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

    /** How a powered shelf is joined with its neighbours (named as seen from the front). */
    public enum SideChain implements StringRepresentable {
        UNCONNECTED("unconnected"),
        RIGHT("right"),
        CENTER("center"),
        LEFT("left");

        private final String name;

        SideChain(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
