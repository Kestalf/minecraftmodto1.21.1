package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.block.StoneFamily;
import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
        super(output, lookup, FutureBackport.MOD_ID, helper);
    }

    private void add(TagKey<Block> tag, DeferredBlock<?>... blocks) {
        var appender = tag(tag);
        for (DeferredBlock<?> b : blocks) appender.add(b.get());
    }

    private void add(TagKey<Block> tag, List<? extends DeferredBlock<?>> blocks) {
        var appender = tag(tag);
        for (DeferredBlock<?> b : blocks) appender.add(b.get());
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        wood(ModBlocks.PALE_OAK, ModTags.Blocks.PALE_OAK_LOGS);
        wood(ModBlocks.POPLAR, ModTags.Blocks.POPLAR_LOGS);

        for (StoneFamily family : List.of(ModBlocks.RESIN_BRICKS, ModBlocks.CINNABAR, ModBlocks.POLISHED_CINNABAR, ModBlocks.CINNABAR_BRICKS,
                ModBlocks.SULFUR, ModBlocks.POLISHED_SULFUR, ModBlocks.SULFUR_BRICKS)) {
            add(BlockTags.MINEABLE_WITH_PICKAXE, family.base(), family.stairs(), family.slab(), family.wall());
            add(BlockTags.STAIRS, family.stairs());
            add(BlockTags.SLABS, family.slab());
            add(BlockTags.WALLS, family.wall());
        }
        add(BlockTags.MINEABLE_WITH_PICKAXE, ModBlocks.CHISELED_RESIN_BRICKS, ModBlocks.CHISELED_CINNABAR, ModBlocks.CHISELED_SULFUR);

        for (var color : net.minecraft.world.item.DyeColor.values()) {
            var woolStairs = ModBlocks.WOOL_STAIRS.get(color);
            var woolSlab = ModBlocks.WOOL_SLABS.get(color);
            var concreteStairs = ModBlocks.CONCRETE_STAIRS.get(color);
            var concreteSlab = ModBlocks.CONCRETE_SLABS.get(color);
            add(BlockTags.STAIRS, woolStairs, concreteStairs);
            add(BlockTags.SLABS, woolSlab, concreteSlab);
            add(BlockTags.DAMPENS_VIBRATIONS, woolStairs, woolSlab);
            add(BlockTags.OCCLUDES_VIBRATION_SIGNALS, woolStairs, woolSlab);
            add(BlockTags.MINEABLE_WITH_PICKAXE, concreteStairs, concreteSlab);
        }

        add(BlockTags.MINEABLE_WITH_PICKAXE, ModBlocks.COPPER.all().stream().filter(b -> b != ModBlocks.COPPER.torch && b != ModBlocks.COPPER.wallTorch).toList());

        // Plants and pale garden
        add(BlockTags.DIRT, ModBlocks.PALE_MOSS_BLOCK);
        add(BlockTags.MOSS_REPLACEABLE, ModBlocks.PALE_MOSS_BLOCK);
        add(BlockTags.MINEABLE_WITH_HOE, ModBlocks.PALE_MOSS_BLOCK, ModBlocks.PALE_MOSS_CARPET);
        add(BlockTags.SMALL_FLOWERS, ModBlocks.OPEN_EYEBLOSSOM, ModBlocks.CLOSED_EYEBLOSSOM, ModBlocks.GOLDEN_DANDELION, ModBlocks.CACTUS_FLOWER);
        add(BlockTags.FLOWERS, ModBlocks.WILDFLOWERS);
        add(BlockTags.FLOWER_POTS, ModBlocks.POTTED_OPEN_EYEBLOSSOM, ModBlocks.POTTED_CLOSED_EYEBLOSSOM, ModBlocks.POTTED_GOLDEN_DANDELION);
        add(BlockTags.REPLACEABLE_BY_TREES, ModBlocks.LEAF_LITTER, ModBlocks.WILDFLOWERS, ModBlocks.BUSH, ModBlocks.FIREFLY_BUSH,
                ModBlocks.SHORT_DRY_GRASS, ModBlocks.TALL_DRY_GRASS, ModBlocks.PALE_MOSS_CARPET, ModBlocks.PALE_HANGING_MOSS,
                ModBlocks.RED_SHRUB);
        add(BlockTags.SWORD_EFFICIENT, ModBlocks.LEAF_LITTER, ModBlocks.BUSH, ModBlocks.FIREFLY_BUSH, ModBlocks.SHORT_DRY_GRASS,
                ModBlocks.TALL_DRY_GRASS, ModBlocks.PALE_HANGING_MOSS, ModBlocks.RED_SHRUB, ModBlocks.CACTUS_FLOWER, ModBlocks.SHELF_MUSHROOM);
        add(BlockTags.INSIDE_STEP_SOUND_BLOCKS, ModBlocks.LEAF_LITTER);
        add(BlockTags.COMBINATION_STEP_SOUND_BLOCKS, ModBlocks.PALE_MOSS_CARPET);
        add(BlockTags.ENDERMAN_HOLDABLE, ModBlocks.OPEN_EYEBLOSSOM, ModBlocks.CLOSED_EYEBLOSSOM);
        add(BlockTags.MINEABLE_WITH_AXE, ModBlocks.SHELF_MUSHROOM);
        tag(BlockTags.BEE_GROWABLES);
        tag(ModTags.Blocks.LIGHTNING_RODS).add(Blocks.LIGHTNING_ROD);
        add(ModTags.Blocks.LIGHTNING_RODS, ModBlocks.COPPER.lightningRods.all());
    }

    private void wood(WoodFamily f, TagKey<Block> logsTag) {
        add(logsTag, f.log, f.wood, f.strippedLog, f.strippedWood);
        tag(BlockTags.LOGS_THAT_BURN).addTag(logsTag);
        tag(BlockTags.OVERWORLD_NATURAL_LOGS).add(f.log.get());
        add(BlockTags.PLANKS, f.planks);
        add(BlockTags.WOODEN_STAIRS, f.stairs);
        add(BlockTags.WOODEN_SLABS, f.slab);
        add(BlockTags.WOODEN_FENCES, f.fence);
        add(BlockTags.FENCE_GATES, f.fenceGate);
        add(BlockTags.WOODEN_DOORS, f.door);
        add(BlockTags.WOODEN_TRAPDOORS, f.trapdoor);
        add(BlockTags.WOODEN_BUTTONS, f.button);
        add(BlockTags.WOODEN_PRESSURE_PLATES, f.pressurePlate);
        add(BlockTags.STANDING_SIGNS, f.sign);
        add(BlockTags.WALL_SIGNS, f.wallSign);
        add(BlockTags.CEILING_HANGING_SIGNS, f.hangingSign);
        add(BlockTags.WALL_HANGING_SIGNS, f.wallHangingSign);
        add(BlockTags.SAPLINGS, f.sapling);
        add(BlockTags.FLOWER_POTS, f.pottedSapling);
        add(BlockTags.LEAVES, f.leaves);
        add(BlockTags.MINEABLE_WITH_HOE, f.leaves);
        add(BlockTags.MINEABLE_WITH_AXE, f.log, f.wood, f.strippedLog, f.strippedWood, f.planks, f.stairs, f.slab, f.fence, f.fenceGate,
                f.door, f.trapdoor, f.button, f.pressurePlate, f.sign, f.wallSign, f.hangingSign, f.wallHangingSign);
        tag(BlockTags.COMPLETES_FIND_TREE_TUTORIAL).addTag(logsTag);
    }
}
