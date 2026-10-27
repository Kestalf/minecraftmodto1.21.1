package io.github.kestalf.futurebackport.entity.golem;

import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Empty-handed golems take up to 16 of the first item from the nearest unvisited copper chest; golems holding items
 * put them into the nearest unvisited wooden chest that is empty or already holds that item. Each visit takes three
 * seconds. After ten fruitless visits the golem wanders for seven seconds and forgets what it visited.
 */
class TransportItemsGoal extends Goal {
    private static final int HORIZONTAL_RANGE = 32;
    private static final int VERTICAL_RANGE = 8;
    private static final int INTERACTION_TICKS = 60;
    private static final int TRANSFER_TICK = 30;
    private static final int MAX_VISITED = 10;
    private static final int GIVE_UP_TICKS = 140;
    private static final int MAX_TAKEN = 16;
    private static final double REACH_SQ = 2.25 * 2.25;

    private final CopperGolem golem;
    private final List<BlockPos> visited = new ArrayList<>();
    @Nullable
    private BlockPos target;
    private int interactionTicks = -1;
    private int giveUpTicks;
    private int searchCooldown;
    private int stuckTicks;
    /** Whether the golem held items when it opened the current chest. */
    private boolean carrying;

    TransportItemsGoal(CopperGolem golem) {
        this.golem = golem;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean holdingItem() {
        return !golem.getMainHandItem().isEmpty();
    }

    @Override
    public boolean canUse() {
        if (giveUpTicks > 0) {
            if (--giveUpTicks == 0) visited.clear();
            return false;
        }
        if (searchCooldown-- > 0) return false;
        searchCooldown = 20;
        target = findTarget();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && (interactionTicks >= 0 || giveUpTicks == 0);
    }

    @Override
    public void start() {
        interactionTicks = -1;
        stuckTicks = 0;
        moveToTarget();
    }

    @Override
    public void stop() {
        if (interactionTicks >= 0 && target != null) closeChest(target);
        golem.setInteraction(CopperGolem.Interaction.NONE);
        golem.getNavigation().stop();
        interactionTicks = -1;
        target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (target == null) return;
        BlockState state = golem.level().getBlockState(target);
        if (!isValidTarget(state) && interactionTicks < 0) {
            markVisited(target);
            target = null;
            return;
        }
        golem.getLookControl().setLookAt(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
        if (interactionTicks < 0) {
            if (golem.distanceToSqr(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5) <= REACH_SQ && canReachVertically(target)) {
                golem.getNavigation().stop();
                if (ChestBlock.isChestBlockedAt(golem.level(), target)) {
                    markVisited(target);
                    target = null;
                    return;
                }
                interactionTicks = 0;
                openChest(target);
                Container container = container(target);
                carrying = holdingItem();
                golem.setInteraction(carrying
                        ? (container != null && canPut(container) ? CopperGolem.Interaction.PUTTING : CopperGolem.Interaction.PUTTING_FAILED)
                        : (container != null && canTake(container) ? CopperGolem.Interaction.TAKING : CopperGolem.Interaction.TAKING_FAILED));
            } else if (golem.getNavigation().isDone()) {
                if (++stuckTicks > 40) {
                    markVisited(target);
                    target = null;
                } else {
                    moveToTarget();
                }
            }
            return;
        }
        interactionTicks++;
        if (interactionTicks == TRANSFER_TICK) transfer(target);
        if (interactionTicks >= INTERACTION_TICKS) {
            closeChest(target);
            golem.setInteraction(CopperGolem.Interaction.NONE);
            interactionTicks = -1;
            target = null;
        }
    }

    private boolean canReachVertically(BlockPos pos) {
        int dy = pos.getY() - golem.blockPosition().getY();
        return dy <= 1 && dy >= -2;
    }

    private void moveToTarget() {
        if (target == null) return;
        Path path = golem.getNavigation().createPath(target, 1);
        golem.getNavigation().moveTo(path, 1.0);
    }

    private boolean isValidTarget(BlockState state) {
        return holdingItem() ? state.is(Tags.Blocks.CHESTS_WOODEN) && !state.is(ModTags.Blocks.COPPER_CHESTS)
                : state.is(ModTags.Blocks.COPPER_CHESTS);
    }

    @Nullable
    private BlockPos findTarget() {
        BlockPos origin = golem.blockPosition();
        ServerLevel level = (ServerLevel) golem.level();
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-HORIZONTAL_RANGE, -VERTICAL_RANGE, -HORIZONTAL_RANGE),
                origin.offset(HORIZONTAL_RANGE, VERTICAL_RANGE, HORIZONTAL_RANGE))) {
            if (!level.isLoaded(pos)) continue;
            if (visited.contains(pos)) continue;
            if (level.getBlockEntity(pos) instanceof ChestBlockEntity && isValidTarget(level.getBlockState(pos))) {
                candidates.add(pos.immutable());
            }
        }
        return candidates.stream().min(Comparator.comparingDouble(pos -> pos.distSqr(origin))).orElse(null);
    }

    private void markVisited(BlockPos pos) {
        visited.add(pos);
        if (visited.size() >= MAX_VISITED) giveUpTicks = GIVE_UP_TICKS;
    }

    @Nullable
    private Container container(BlockPos pos) {
        BlockState state = golem.level().getBlockState(pos);
        return state.getBlock() instanceof ChestBlock chest ? ChestBlock.getContainer(chest, state, golem.level(), pos, true) : null;
    }

    private void openChest(BlockPos pos) {
        signalOpeners(pos, 1);
    }

    private void closeChest(BlockPos pos) {
        signalOpeners(pos, 0);
    }

    /** Opens or closes the lid by adding this golem to the chest's opener count (the chest only counts players). */
    private void signalOpeners(BlockPos pos, int golemOpeners) {
        BlockState state = golem.level().getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock)) return;
        int players = ChestBlockEntity.getOpenCount(golem.level(), pos);
        golem.level().blockEvent(pos, state.getBlock(), 1, players + golemOpeners);
        if (players == 0) {
            golem.level().playSound(null, pos, golemOpeners > 0 ? net.minecraft.sounds.SoundEvents.CHEST_OPEN
                    : net.minecraft.sounds.SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, golem.getRandom().nextFloat() * 0.1F + 0.9F);
        }
    }

    private void transfer(BlockPos pos) {
        Container container = container(pos);
        boolean success = container != null && (carrying ? put(container) : take(container));
        if (success) {
            visited.clear();
        } else {
            markVisited(pos);
        }
        String sound = carrying ? (success ? "item_drop" : "item_no_drop") : (success ? "no_item_get" : "no_item_no_get");
        golem.playSound(golem.sound(sound), 1.0F, 1.0F);
    }

    private static boolean canTake(Container container) {
        return !container.isEmpty();
    }

    private boolean canPut(Container container) {
        ItemStack held = golem.getMainHandItem();
        boolean empty = true;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            empty = false;
            if (stack.is(held.getItem())) return true;
        }
        return empty;
    }

    private boolean take(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) continue;
            ItemStack taken = container.removeItem(slot, Math.min(MAX_TAKEN, stack.getCount()));
            golem.setItemSlot(EquipmentSlot.MAINHAND, taken);
            golem.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            container.setChanged();
            return true;
        }
        return false;
    }

    private boolean put(Container container) {
        if (!canPut(container)) return false;
        ItemStack held = golem.getMainHandItem().copy();
        int before = held.getCount();
        for (int slot = 0; slot < container.getContainerSize() && !held.isEmpty(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                container.setItem(slot, held.split(Math.min(held.getCount(), container.getMaxStackSize(held))));
            } else if (ItemStack.isSameItemSameComponents(stack, held) && stack.getCount() < stack.getMaxStackSize()) {
                int moved = Math.min(held.getCount(), stack.getMaxStackSize() - stack.getCount());
                stack.grow(moved);
                held.shrink(moved);
            }
        }
        golem.setItemSlot(EquipmentSlot.MAINHAND, held);
        container.setChanged();
        return held.getCount() < before;
    }
}
