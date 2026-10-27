package io.github.kestalf.futurebackport.block.plant;

import io.github.kestalf.futurebackport.registry.ModParticles;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;

/** Firefly bush: glows faintly and releases fireflies in the dark. */
public class FireflyBushBlock extends SpreadingBushBlock {
    public FireflyBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean dark = level.isNight() || level.getMaxLocalRawBrightness(pos) <= 13;
        if (random.nextInt(30) == 0 && level.isNight()
                && level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) <= pos.getY()) {
            level.playLocalSound(pos, ModSounds.event("block.firefly_bush.idle").get(), SoundSource.AMBIENT, 1.0F, 1.0F, false);
        }
        if (dark && random.nextInt(7) == 0) {
            double x = pos.getX() + random.nextDouble() * 6.0 - 3.0;
            double y = pos.getY() + random.nextDouble() * 3.0;
            double z = pos.getZ() + random.nextDouble() * 6.0 - 3.0;
            level.addParticle(ModParticles.FIREFLY, x, y, z, 0.0, 0.0, 0.0);
        }
    }
}
