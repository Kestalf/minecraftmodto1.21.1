package io.github.kestalf.futurebackport.block.plant;

import com.mojang.serialization.MapCodec;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/**
 * Short and tall dry grass. They grow on the same blocks as dead bushes, rustle in the wind in deserts and badlands,
 * and bone meal turns short dry grass into tall dry grass (tall dry grass spreads short dry grass around it).
 */
public class DryVegetationBlock extends BushBlock implements BonemealableBlock {
    private final VoxelShape shape;
    private final Supplier<? extends Block> grownInto;
    private final Supplier<? extends Block> spreads;

    /**
     * @param grownInto block that bone meal turns this one into, or {@code null}
     * @param spreads   block that bone meal sprouts next to this one when it cannot grow further
     */
    public DryVegetationBlock(VoxelShape shape, Supplier<? extends Block> grownInto, Supplier<? extends Block> spreads, Properties properties) {
        super(properties);
        this.shape = shape;
        this.grownInto = grownInto;
        this.spreads = spreads;
    }

    @Override
    protected MapCodec<? extends BushBlock> codec() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DEAD_BUSH_MAY_PLACE_ON);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(1600) == 0 && level.getBlockState(pos.below()).is(BlockTags.SAND)
                && level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) <= pos.getY()) {
            level.playLocalSound(pos, ModSounds.event("block.dry_grass.ambient").get(), SoundSource.AMBIENT, 1.0F, 1.0F, false);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (grownInto != null) {
            level.setBlockAndUpdate(pos, grownInto.get().defaultBlockState());
            return;
        }
        if (spreads == null) return;
        BlockState sprout = spreads.get().defaultBlockState();
        for (int i = 0; i < 8; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(3) - 1, random.nextInt(3) - 1);
            if (level.isEmptyBlock(target) && sprout.canSurvive(level, target)) {
                level.setBlockAndUpdate(target, sprout);
                return;
            }
        }
    }
}
