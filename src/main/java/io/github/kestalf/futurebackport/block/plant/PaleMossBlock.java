package io.github.kestalf.futurebackport.block.plant;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.worldgen.ModFeatureKeys;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Pale moss block: bone meal spreads pale moss and grows grass and pale moss carpets, like the regular moss block. */
public class PaleMossBlock extends Block implements BonemealableBlock {
    public static final MapCodec<PaleMossBlock> CODEC = simpleCodec(PaleMossBlock::new);

    public PaleMossBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        level.registryAccess().registry(Registries.CONFIGURED_FEATURE)
                .flatMap(registry -> registry.getHolder(ModFeatureKeys.PALE_MOSS_PATCH_BONEMEAL))
                .ifPresent(feature -> feature.value().place(level, level.getChunkSource().getGenerator(), random, pos.above()));
    }

    @Override
    public Type getType() {
        return Type.NEIGHBOR_SPREADER;
    }
}
