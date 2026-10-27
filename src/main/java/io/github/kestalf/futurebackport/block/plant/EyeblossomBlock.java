package io.github.kestalf.futurebackport.block.plant;

import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Eyeblossoms open at night and close during the day. When one changes, nearby eyeblossoms follow a moment later.
 */
public class EyeblossomBlock extends FlowerBlock {
    private final boolean open;

    public EyeblossomBlock(boolean open, Properties properties) {
        super(open ? MobEffects.BLINDNESS : MobEffects.CONFUSION, open ? 11.0F : 7.0F, properties);
        this.open = open;
    }

    public boolean isOpen() {
        return open;
    }

    private static boolean shouldBeOpen(Level level) {
        return level.isNight();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.dimensionType().hasFixedTime() && shouldBeOpen(level) != open) {
            transform(level, pos, true);
            // Neighbouring eyeblossoms follow with a short, random delay.
            BlockPos.betweenClosed(pos.offset(-3, -2, -3), pos.offset(3, 2, 3)).forEach(other -> {
                if (!other.equals(pos) && level.getBlockState(other).getBlock() instanceof EyeblossomBlock eye && eye.open == open) {
                    level.scheduleTick(other.immutable(), eye, 5 + random.nextInt(25));
                }
            });
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.dimensionType().hasFixedTime() && shouldBeOpen(level) != open) {
            transform(level, pos, false);
        }
    }

    private void transform(ServerLevel level, BlockPos pos, boolean first) {
        Block target = (open ? ModBlocks.CLOSED_EYEBLOSSOM : ModBlocks.OPEN_EYEBLOSSOM).get();
        level.setBlock(pos, target.defaultBlockState(), Block.UPDATE_ALL);
        String sound = "block.eyeblossom." + (open ? "close" : "open") + (first ? "_long" : "");
        SoundEvent event = ModSounds.event(sound).get();
        level.playSound(null, pos, event, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (open && random.nextInt(700) == 0 && level.getBlockState(pos.below()).is(ModBlocks.PALE_MOSS_BLOCK)) {
            level.playLocalSound(pos, ModSounds.event("block.eyeblossom.idle").get(), SoundSource.AMBIENT, 1.0F, 1.0F, false);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide && open && level.getDifficulty() != Difficulty.PEACEFUL && entity instanceof Bee bee
                && !bee.hasEffect(MobEffects.POISON)) {
            bee.addEffect(new MobEffectInstance(MobEffects.POISON, 25));
        }
    }
}
