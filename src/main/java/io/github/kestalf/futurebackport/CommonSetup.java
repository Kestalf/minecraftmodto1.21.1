package io.github.kestalf.futurebackport;

import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.List;

public final class CommonSetup {
    static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            for (WoodFamily family : List.of(ModBlocks.PALE_OAK, ModBlocks.POPLAR)) {
                family.forEachFlammable(fire::setFlammable);
            }
            for (var block : List.of(ModBlocks.PALE_MOSS_CARPET, ModBlocks.PALE_HANGING_MOSS, ModBlocks.LEAF_LITTER, ModBlocks.BUSH,
                    ModBlocks.FIREFLY_BUSH, ModBlocks.SHORT_DRY_GRASS, ModBlocks.TALL_DRY_GRASS, ModBlocks.WILDFLOWERS,
                    ModBlocks.RED_SHRUB, ModBlocks.CACTUS_FLOWER, ModBlocks.SHELF_MUSHROOM)) {
                fire.setFlammable(block.get(), 60, 100);
            }
            fire.setFlammable(ModBlocks.PALE_MOSS_BLOCK.get(), 5, 100);
            net.minecraft.world.level.block.DispenserBlock.registerProjectileBehavior(io.github.kestalf.futurebackport.registry.ModItems.BLUE_EGG.get());
            net.minecraft.world.level.block.DispenserBlock.registerProjectileBehavior(io.github.kestalf.futurebackport.registry.ModItems.BROWN_EGG.get());
            ModBlocks.SHELVES.forEach((wood, shelf) -> {
                if (!ModBlocks.isNetherWood(wood)) fire.setFlammable(shelf.get(), 30, 20);
            });
            ModBlocks.WOOL_STAIRS.values().forEach(b -> fire.setFlammable(b.get(), 30, 60));
            ModBlocks.WOOL_SLABS.values().forEach(b -> fire.setFlammable(b.get(), 30, 60));

            FlowerPotBlock pot = (FlowerPotBlock) Blocks.FLOWER_POT;
            pot.addPlant(ModBlocks.PALE_OAK.sapling.getId(), ModBlocks.PALE_OAK.pottedSapling);
            pot.addPlant(ModBlocks.POPLAR.sapling.getId(), ModBlocks.POPLAR.pottedSapling);
            pot.addPlant(ModBlocks.OPEN_EYEBLOSSOM.getId(), ModBlocks.POTTED_OPEN_EYEBLOSSOM);
            pot.addPlant(ModBlocks.CLOSED_EYEBLOSSOM.getId(), ModBlocks.POTTED_CLOSED_EYEBLOSSOM);
            pot.addPlant(ModBlocks.GOLDEN_DANDELION.getId(), ModBlocks.POTTED_GOLDEN_DANDELION);
        });
    }

    static void onBlockEntityTypeAddBlocks(BlockEntityTypeAddBlocksEvent event) {
        for (WoodFamily family : List.of(ModBlocks.PALE_OAK, ModBlocks.POPLAR)) {
            event.modify(BlockEntityType.SIGN, family.sign.get(), family.wallSign.get());
            event.modify(BlockEntityType.HANGING_SIGN, family.hangingSign.get(), family.wallHangingSign.get());
        }
    }

    static void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        VanillaTabPlacement.populate(event);
    }

    private CommonSetup() {
    }
}
