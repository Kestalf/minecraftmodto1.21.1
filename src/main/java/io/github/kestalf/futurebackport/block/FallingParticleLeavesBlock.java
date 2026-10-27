package io.github.kestalf.futurebackport.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Leaves that drop their own falling leaf particles, like cherry leaves do. */
public class FallingParticleLeavesBlock extends LeavesBlock {
    private final Supplier<? extends ParticleOptions> particle;

    public FallingParticleLeavesBlock(Supplier<? extends ParticleOptions> particle, Properties properties) {
        super(properties);
        this.particle = particle;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(10) == 0) {
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (!isFaceFull(belowState.getCollisionShape(level, below), Direction.UP)) {
                ParticleUtils.spawnParticleBelow(level, pos, random, particle.get());
            }
        }
    }
}
