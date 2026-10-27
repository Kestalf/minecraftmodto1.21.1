package io.github.kestalf.futurebackport.block;

import io.github.kestalf.futurebackport.registry.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** A full block with its stairs, slab and wall (e.g. cinnabar bricks + cinnabar brick stairs/slab/wall). */
public record StoneFamily(DeferredBlock<Block> base, DeferredBlock<StairBlock> stairs, DeferredBlock<SlabBlock> slab,
                          DeferredBlock<WallBlock> wall) {

    /**
     * @param baseName name of the full block, e.g. {@code cinnabar_bricks}
     * @param prefix   prefix of the variants, e.g. {@code cinnabar_brick}
     */
    public static StoneFamily of(String baseName, String prefix, BlockBehaviour.Properties properties) {
        DeferredBlock<Block> base = ModBlocks.block(baseName, () -> new Block(properties));
        DeferredBlock<StairBlock> stairs = ModBlocks.block(prefix + "_stairs",
                () -> new StairBlock(base.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(base.get())));
        DeferredBlock<SlabBlock> slab = ModBlocks.block(prefix + "_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(base.get())));
        DeferredBlock<WallBlock> wall = ModBlocks.block(prefix + "_wall",
                () -> new WallBlock(BlockBehaviour.Properties.ofFullCopy(base.get()).forceSolidOn()));
        return new StoneFamily(base, stairs, slab, wall);
    }
}
