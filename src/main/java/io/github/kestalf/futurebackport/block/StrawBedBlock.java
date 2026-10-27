package io.github.kestalf.futurebackport.block;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Straw bed: sleeping in it skips the night without setting the spawn point (see {@code StrawBedEvents}) and the bed
 * falls apart, without drops, once its sleeper gets up. It does not bounce or soften falls and never explodes.
 */
public class StrawBedBlock extends BedBlock {
    public static final MapCodec<StrawBedBlock> CODEC = simpleCodec(StrawBedBlock::new);

    public StrawBedBlock(Properties properties) {
        super(DyeColor.YELLOW, properties);
    }

    @Override
    @SuppressWarnings("unchecked")
    public MapCodec<BedBlock> codec() {
        return (MapCodec<BedBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && !BedBlock.canSetSpawn(level)) {
            // In the Nether or the End it just falls apart.
            removeBed(level, pos, state, false);
            return InteractionResult.SUCCESS;
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && oldState.is(this) && oldState.getValue(OCCUPIED) && !state.getValue(OCCUPIED)) {
            // The sleeper got up: break once they have stood up.
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OCCUPIED)) removeBed(level, pos, state, true);
    }

    private static void removeBed(Level level, BlockPos pos, BlockState state, boolean afterSleep) {
        BlockPos other = pos.relative(getConnectedDirection(state));
        level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        level.playSound(null, pos, ModSounds.event(afterSleep ? "block.straw_bed.break_leave" : "block.straw_bed.break").get(),
                SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0));
    }

    public static boolean isHead(BlockState state) {
        return state.getValue(PART) == BedPart.HEAD;
    }
}
