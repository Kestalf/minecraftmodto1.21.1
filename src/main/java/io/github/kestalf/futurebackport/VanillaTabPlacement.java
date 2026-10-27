package io.github.kestalf.futurebackport;

import io.github.kestalf.futurebackport.block.StoneFamily;
import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModItems;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import java.util.ArrayList;
import java.util.List;

/** Adds the new items to the vanilla creative tabs next to where they are in the newest release. */
final class VanillaTabPlacement {
    static void populate(BuildCreativeModeTabContentsEvent event) {
        var tab = event.getTabKey();
        if (tab == CreativeModeTabs.BUILDING_BLOCKS) {
            after(event, Items.CHERRY_BUTTON, woodBlocks(ModBlocks.PALE_OAK));
            after(event, Items.DARK_OAK_BUTTON, woodBlocks(ModBlocks.POPLAR));
            after(event, Items.MUD_BRICK_WALL, stone(ModBlocks.RESIN_BRICKS, ModBlocks.CHISELED_RESIN_BRICKS.get()));
            List<ItemLike> minerals = new ArrayList<>();
            minerals.addAll(stone(ModBlocks.CINNABAR, null));
            minerals.addAll(stone(ModBlocks.POLISHED_CINNABAR, null));
            minerals.addAll(stone(ModBlocks.CINNABAR_BRICKS, ModBlocks.CHISELED_CINNABAR.get()));
            minerals.addAll(stone(ModBlocks.SULFUR, null));
            minerals.addAll(stone(ModBlocks.POLISHED_SULFUR, null));
            minerals.addAll(stone(ModBlocks.SULFUR_BRICKS, ModBlocks.CHISELED_SULFUR.get()));
            after(event, Items.CHISELED_TUFF_BRICKS, minerals);
            for (WeatherState state : WeatherState.values()) {
                ItemLike anchor = switch (state) {
                    case UNAFFECTED -> Items.COPPER_BULB;
                    case EXPOSED -> Items.EXPOSED_COPPER_BULB;
                    case WEATHERED -> Items.WEATHERED_COPPER_BULB;
                    case OXIDIZED -> Items.OXIDIZED_COPPER_BULB;
                };
                after(event, anchor, List.of(ModBlocks.COPPER.bars.unwaxed.get(state).get(), ModBlocks.COPPER.chains.unwaxed.get(state).get()));
            }
        } else if (tab == CreativeModeTabs.COLORED_BLOCKS) {
            for (DyeColor color : DyeColor.values()) {
                after(event, ModBlocks.VanillaColored.wool(color),
                        List.of(ModBlocks.WOOL_STAIRS.get(color).get(), ModBlocks.WOOL_SLABS.get(color).get()));
                after(event, ModBlocks.VanillaColored.concrete(color),
                        List.of(ModBlocks.CONCRETE_STAIRS.get(color).get(), ModBlocks.CONCRETE_SLABS.get(color).get()));
            }
        } else if (tab == CreativeModeTabs.NATURAL_BLOCKS) {
            after(event, Items.CHERRY_LOG, List.of(ModBlocks.PALE_OAK.log.get()));
            after(event, Items.DARK_OAK_LOG, List.of(ModBlocks.POPLAR.log.get()));
            after(event, Items.CHERRY_LEAVES, List.of(ModBlocks.PALE_OAK.leaves.getFirst().get()));
            after(event, Items.DARK_OAK_LEAVES, ModBlocks.POPLAR.leaves.stream().map(b -> (ItemLike) b.get()).toList());
            after(event, Items.CHERRY_SAPLING, List.of(ModBlocks.PALE_OAK.sapling.get()));
            after(event, Items.DARK_OAK_SAPLING, List.of(ModBlocks.POPLAR.sapling.get()));
            after(event, Items.MOSS_CARPET, List.of(ModBlocks.PALE_MOSS_BLOCK.get(), ModBlocks.PALE_MOSS_CARPET.get(), ModBlocks.PALE_HANGING_MOSS.get()));
            after(event, Items.DANDELION, List.of(ModBlocks.GOLDEN_DANDELION.get()));
            after(event, Items.TORCHFLOWER, List.of(ModBlocks.CLOSED_EYEBLOSSOM.get(), ModBlocks.OPEN_EYEBLOSSOM.get()));
            after(event, Items.PINK_PETALS, List.of(ModBlocks.WILDFLOWERS.get(), ModBlocks.LEAF_LITTER.get()));
            after(event, Items.FERN, List.of(ModBlocks.BUSH.get(), ModBlocks.FIREFLY_BUSH.get(), ModBlocks.RED_SHRUB.get()));
            after(event, Items.DEAD_BUSH, List.of(ModBlocks.SHORT_DRY_GRASS.get(), ModBlocks.TALL_DRY_GRASS.get()));
            after(event, Items.CACTUS, List.of(ModBlocks.CACTUS_FLOWER.get()));
            after(event, Items.BROWN_MUSHROOM, List.of(ModBlocks.SHELF_MUSHROOM.get()));
            after(event, Items.GLOW_LICHEN, List.of(ModBlocks.RESIN_CLUMP.get(), ModBlocks.RESIN_BLOCK.get()));
            after(event, Items.TUFF, List.of(ModBlocks.CINNABAR.base().get(), ModBlocks.SULFUR.base().get()));
        } else if (tab == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            after(event, Items.SOUL_TORCH, List.of(ModBlocks.COPPER.torchItem.get()));
            List<ItemLike> lanterns = new ArrayList<>();
            ModBlocks.COPPER.lanterns.unwaxed.values().forEach(b -> lanterns.add(b.get()));
            after(event, Items.SOUL_LANTERN, lanterns);
            after(event, Items.CHERRY_HANGING_SIGN, List.of(ModBlocks.PALE_OAK.signItem.get(), ModBlocks.PALE_OAK.hangingSignItem.get(),
                    ModBlocks.POPLAR.signItem.get(), ModBlocks.POPLAR.hangingSignItem.get()));
        } else if (tab == CreativeModeTabs.REDSTONE_BLOCKS) {
            List<ItemLike> rods = new ArrayList<>();
            ModBlocks.COPPER.lightningRods.unwaxed.values().forEach(b -> rods.add(b.get()));
            after(event, Items.LIGHTNING_ROD, rods);
        } else if (tab == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            after(event, Items.CHERRY_CHEST_BOAT, List.of(ModBlocks.PALE_OAK.boat.get(), ModBlocks.PALE_OAK.chestBoat.get(),
                    ModBlocks.POPLAR.boat.get(), ModBlocks.POPLAR.chestBoat.get()));
        } else if (tab == CreativeModeTabs.INGREDIENTS) {
            after(event, Items.IRON_NUGGET, List.of(ModItems.COPPER_NUGGET.get()));
            after(event, Items.NETHER_BRICK, List.of(ModItems.RESIN_BRICK.get()));
        }
    }

    private static List<ItemLike> woodBlocks(WoodFamily f) {
        return List.of(f.log.get(), f.wood.get(), f.strippedLog.get(), f.strippedWood.get(), f.planks.get(), f.stairs.get(),
                f.slab.get(), f.fence.get(), f.fenceGate.get(), f.door.get(), f.trapdoor.get(), f.pressurePlate.get(), f.button.get());
    }

    private static List<ItemLike> stone(StoneFamily family, ItemLike chiseled) {
        List<ItemLike> list = new ArrayList<>(List.of(family.base().get(), family.stairs().get(), family.slab().get(), family.wall().get()));
        if (chiseled != null) list.add(chiseled);
        return list;
    }

    private static void after(BuildCreativeModeTabContentsEvent event, ItemLike anchor, List<? extends ItemLike> items) {
        ItemStack previous = new ItemStack(anchor);
        for (ItemLike item : items) {
            ItemStack stack = new ItemStack(item);
            try {
                event.insertAfter(previous, stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            } catch (IllegalArgumentException e) {
                // The anchor is not in this tab (e.g. hidden by a feature flag): append instead.
                event.accept(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
            previous = stack;
        }
    }

    private VanillaTabPlacement() {
    }
}
