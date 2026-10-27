package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.entity.sulfur.SulfurCubeArchetype;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModItems;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {
    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                               CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper helper) {
        super(output, lookup, blockTags, FutureBackport.MOD_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        copy(ModTags.Blocks.PALE_OAK_LOGS, ModTags.Items.PALE_OAK_LOGS);
        copy(ModTags.Blocks.POPLAR_LOGS, ModTags.Items.POPLAR_LOGS);
        copy(ModTags.Blocks.WOODEN_SHELVES, ModTags.Items.WOODEN_SHELVES);
        tag(ModTags.Items.NAUTILUS_BUCKET_FOOD).add(Items.PUFFERFISH_BUCKET, Items.COD_BUCKET, Items.SALMON_BUCKET, Items.TROPICAL_FISH_BUCKET);
        tag(ModTags.Items.NAUTILUS_FOOD).addTag(ItemTags.FISHES).addTag(ModTags.Items.NAUTILUS_BUCKET_FOOD);
        tag(ModTags.Items.NAUTILUS_TAMING_ITEMS).add(Items.PUFFERFISH_BUCKET, Items.PUFFERFISH);
        tag(ModTags.Items.SULFUR_CUBE_FOOD).add(Items.SLIME_BALL);
        var swallowable = tag(ModTags.Items.SULFUR_CUBE_SWALLOWABLE);
        for (SulfurCubeArchetype archetype : SulfurCubeArchetype.values()) swallowable.addTag(archetype.items());
        // The 26.3 archetype lists, translated to 1.21.1 ids (new blocks are the mod's, missing tags are spelled out).
        archetype(SulfurCubeArchetype.BOUNCY,
                "#minecraft:planks", "minecraft:bamboo_mosaic", "#minecraft:logs", "#minecraft:bamboo_blocks");
        archetype(SulfurCubeArchetype.EXPLOSIVE,
                "minecraft:tnt");
        archetype(SulfurCubeArchetype.FAST_FLAT,
                "minecraft:tube_coral_block", "minecraft:brain_coral_block", "minecraft:bubble_coral_block",
                "minecraft:fire_coral_block", "minecraft:horn_coral_block", "minecraft:dead_tube_coral_block",
                "minecraft:dead_brain_coral_block", "minecraft:dead_bubble_coral_block", "minecraft:dead_fire_coral_block",
                "minecraft:dead_horn_coral_block", "minecraft:sponge", "minecraft:wet_sponge", "minecraft:dried_kelp_block",
                "#minecraft:moss_blocks", "minecraft:resin_block", "minecraft:resin_bricks",
                "minecraft:chiseled_resin_bricks", "minecraft:melon", "minecraft:hay_block", "minecraft:pumpkin",
                "minecraft:carved_pumpkin", "minecraft:jack_o_lantern", "minecraft:ochre_froglight",
                "minecraft:pearlescent_froglight", "minecraft:verdant_froglight");
        archetype(SulfurCubeArchetype.FAST_SLIDING,
                "minecraft:blue_ice", "minecraft:packed_ice", "minecraft:snow_block");
        archetype(SulfurCubeArchetype.HIGH_RESISTANCE,
                "minecraft:soul_sand", "minecraft:soul_soil");
        archetype(SulfurCubeArchetype.HOT,
                "minecraft:magma_block");
        archetype(SulfurCubeArchetype.LIGHT,
                "#minecraft:wool");
        archetype(SulfurCubeArchetype.REGULAR,
                "#minecraft:concrete_powders", "minecraft:mud", "minecraft:muddy_mangrove_roots", "minecraft:packed_mud",
                "minecraft:coal_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
                "minecraft:podzol", "minecraft:grass_block", "minecraft:clay", "minecraft:bone_block");
        archetype(SulfurCubeArchetype.SLOW_BOUNCY,
                "minecraft:amethyst_block", "minecraft:andesite", "minecraft:basalt", "minecraft:blackstone",
                "minecraft:bricks", "minecraft:calcite", "minecraft:chiseled_cinnabar", "minecraft:chiseled_deepslate",
                "minecraft:chiseled_nether_bricks", "minecraft:chiseled_polished_blackstone",
                "minecraft:chiseled_quartz_block", "minecraft:chiseled_red_sandstone", "minecraft:chiseled_sandstone",
                "minecraft:chiseled_stone_bricks", "minecraft:chiseled_sulfur", "minecraft:chiseled_tuff",
                "minecraft:chiseled_tuff_bricks", "minecraft:cinnabar", "minecraft:cinnabar_bricks",
                "minecraft:cobbled_deepslate", "minecraft:cobblestone", "minecraft:cracked_deepslate_bricks",
                "minecraft:cracked_deepslate_tiles", "minecraft:cracked_nether_bricks",
                "minecraft:cracked_polished_blackstone_bricks", "minecraft:cracked_stone_bricks", "minecraft:crimson_nylium",
                "minecraft:crying_obsidian", "minecraft:cut_red_sandstone", "minecraft:cut_sandstone",
                "minecraft:dark_prismarine", "minecraft:deepslate", "minecraft:deepslate_bricks",
                "minecraft:deepslate_tiles", "minecraft:diamond_block", "minecraft:diorite", "minecraft:dripstone_block",
                "minecraft:emerald_block", "minecraft:end_stone", "minecraft:end_stone_bricks",
                "minecraft:gilded_blackstone", "minecraft:glowstone", "minecraft:granite", "minecraft:lapis_block",
                "minecraft:mossy_cobblestone", "minecraft:mossy_stone_bricks", "minecraft:mud_bricks",
                "minecraft:nether_bricks", "minecraft:netherrack", "minecraft:observer", "minecraft:obsidian",
                "minecraft:polished_andesite", "minecraft:polished_basalt", "minecraft:polished_blackstone",
                "minecraft:polished_blackstone_bricks", "minecraft:polished_cinnabar", "minecraft:polished_deepslate",
                "minecraft:polished_diorite", "minecraft:polished_granite", "minecraft:polished_sulfur",
                "minecraft:polished_tuff", "minecraft:prismarine", "minecraft:prismarine_bricks", "minecraft:purpur_block",
                "minecraft:purpur_pillar", "minecraft:quartz_block", "minecraft:quartz_bricks",
                "minecraft:nether_quartz_ore", "minecraft:quartz_pillar", "minecraft:red_nether_bricks",
                "minecraft:red_sandstone", "minecraft:redstone_lamp", "minecraft:sandstone", "minecraft:sea_lantern",
                "minecraft:smooth_basalt", "minecraft:smooth_quartz", "minecraft:smooth_red_sandstone",
                "minecraft:smooth_sandstone", "minecraft:smooth_stone", "minecraft:stone", "minecraft:stone_bricks",
                "minecraft:sulfur", "minecraft:sulfur_bricks", "minecraft:tuff", "minecraft:tuff_bricks",
                "minecraft:warped_nylium", "#minecraft:concrete", "#minecraft:coal_ores", "#minecraft:lapis_ores",
                "#minecraft:redstone_ores", "#minecraft:diamond_ores", "#minecraft:emerald_ores", "#minecraft:terracotta",
                "#minecraft:glazed_terracotta");
        archetype(SulfurCubeArchetype.SLOW_FLAT,
                "minecraft:iron_block", "minecraft:gold_block", "minecraft:raw_copper_block", "minecraft:raw_gold_block",
                "minecraft:raw_iron_block", "#minecraft:gold_ores", "#minecraft:iron_ores", "#minecraft:copper_ores",
                "minecraft:netherite_block", "minecraft:ancient_debris", "minecraft:copper_block",
                "minecraft:exposed_copper", "minecraft:weathered_copper", "minecraft:oxidized_copper",
                "minecraft:waxed_copper_block", "minecraft:waxed_exposed_copper", "minecraft:waxed_weathered_copper",
                "minecraft:waxed_oxidized_copper", "minecraft:copper_bulb", "minecraft:exposed_copper_bulb",
                "minecraft:weathered_copper_bulb", "minecraft:oxidized_copper_bulb", "minecraft:waxed_copper_bulb",
                "minecraft:waxed_exposed_copper_bulb", "minecraft:waxed_weathered_copper_bulb",
                "minecraft:waxed_oxidized_copper_bulb", "minecraft:cut_copper", "minecraft:exposed_cut_copper",
                "minecraft:weathered_cut_copper", "minecraft:oxidized_cut_copper", "minecraft:waxed_cut_copper",
                "minecraft:waxed_exposed_cut_copper", "minecraft:waxed_weathered_cut_copper",
                "minecraft:waxed_oxidized_cut_copper", "minecraft:chiseled_copper", "minecraft:exposed_chiseled_copper",
                "minecraft:weathered_chiseled_copper", "minecraft:oxidized_chiseled_copper",
                "minecraft:waxed_chiseled_copper", "minecraft:waxed_exposed_chiseled_copper",
                "minecraft:waxed_weathered_chiseled_copper", "minecraft:waxed_oxidized_chiseled_copper");
        archetype(SulfurCubeArchetype.SLOW_SLIDING,
                "minecraft:brown_mushroom_block", "minecraft:red_mushroom_block", "minecraft:mushroom_stem",
                "minecraft:mycelium", "#minecraft:wart_blocks", "minecraft:shroomlight");
        archetype(SulfurCubeArchetype.STICKY,
                "minecraft:honeycomb_block");
        copy(BlockTags.LOGS_THAT_BURN, ItemTags.LOGS_THAT_BURN);
        copy(BlockTags.PLANKS, ItemTags.PLANKS);
        copy(BlockTags.WOODEN_STAIRS, ItemTags.WOODEN_STAIRS);
        copy(BlockTags.WOODEN_SLABS, ItemTags.WOODEN_SLABS);
        copy(BlockTags.WOODEN_FENCES, ItemTags.WOODEN_FENCES);
        copy(BlockTags.FENCE_GATES, ItemTags.FENCE_GATES);
        copy(BlockTags.WOODEN_DOORS, ItemTags.WOODEN_DOORS);
        copy(BlockTags.WOODEN_TRAPDOORS, ItemTags.WOODEN_TRAPDOORS);
        copy(BlockTags.WOODEN_BUTTONS, ItemTags.WOODEN_BUTTONS);
        copy(BlockTags.WOODEN_PRESSURE_PLATES, ItemTags.WOODEN_PRESSURE_PLATES);
        copy(BlockTags.SAPLINGS, ItemTags.SAPLINGS);
        copy(BlockTags.LEAVES, ItemTags.LEAVES);
        copy(BlockTags.STAIRS, ItemTags.STAIRS);
        copy(BlockTags.SLABS, ItemTags.SLABS);
        copy(BlockTags.WALLS, ItemTags.WALLS);
        copy(BlockTags.SMALL_FLOWERS, ItemTags.SMALL_FLOWERS);
        copy(BlockTags.FLOWERS, ItemTags.FLOWERS);
        copy(BlockTags.DIRT, ItemTags.DIRT);
        copy(BlockTags.DAMPENS_VIBRATIONS, ItemTags.DAMPENS_VIBRATIONS);
        copy(BlockTags.COMPLETES_FIND_TREE_TUTORIAL, ItemTags.COMPLETES_FIND_TREE_TUTORIAL);
        for (WoodFamily f : List.of(ModBlocks.PALE_OAK, ModBlocks.POPLAR)) {
            tag(ItemTags.SIGNS).add(f.signItem.get());
            tag(ItemTags.HANGING_SIGNS).add(f.hangingSignItem.get());
            tag(ItemTags.BOATS).add(f.boat.get());
            tag(ItemTags.CHEST_BOATS).add(f.chestBoat.get());
        }
        var spears = tag(ModTags.Items.SPEARS);
        ModItems.SPEARS.forEach(spear -> spears.add(spear.get()));
        tag(ModTags.Items.ENCHANTABLE_LUNGE).addTag(ModTags.Items.SPEARS);
        for (TagKey<Item> enchantable : List.of(ItemTags.SHARP_WEAPON_ENCHANTABLE, ItemTags.WEAPON_ENCHANTABLE, ItemTags.FIRE_ASPECT_ENCHANTABLE,
                ItemTags.DURABILITY_ENCHANTABLE, ItemTags.VANISHING_ENCHANTABLE)) {
            tag(enchantable).addTag(ModTags.Items.SPEARS);
        }
        tag(ItemTags.PIGLIN_LOVED).add(ModItems.GOLDEN_SPEAR.get());
        tag(ItemTags.SWORDS).add(ModItems.COPPER_SWORD.get());
        tag(ItemTags.SHOVELS).add(ModItems.COPPER_SHOVEL.get());
        tag(ItemTags.PICKAXES).add(ModItems.COPPER_PICKAXE.get());
        tag(ItemTags.AXES).add(ModItems.COPPER_AXE.get());
        tag(ItemTags.HOES).add(ModItems.COPPER_HOE.get());
        tag(ItemTags.HEAD_ARMOR).add(ModItems.COPPER_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.COPPER_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.COPPER_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.COPPER_BOOTS.get());
        tag(ItemTags.TRIMMABLE_ARMOR).add(ModItems.COPPER_HELMET.get(), ModItems.COPPER_CHESTPLATE.get(), ModItems.COPPER_LEGGINGS.get(),
                ModItems.COPPER_BOOTS.get());
        var harnesses = tag(ModTags.Items.HARNESSES);
        ModItems.HARNESSES.values().forEach(h -> harnesses.add(h.get()));
        var bundles = tag(ModTags.Items.BUNDLES).add(net.minecraft.world.item.Items.BUNDLE);
        ModItems.BUNDLES.values().forEach(b -> bundles.add(b.get()));
        tag(ModTags.Items.HAPPY_GHAST_FOOD).add(net.minecraft.world.item.Items.SNOWBALL);
        tag(ModTags.Items.HAPPY_GHAST_TEMPT_ITEMS).addTag(ModTags.Items.HAPPY_GHAST_FOOD).addTag(ModTags.Items.HARNESSES);

        TagKey<Item> copperNuggets = ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "nuggets/copper"));
        tag(copperNuggets).add(ModItems.COPPER_NUGGET.get());
        tag(ItemTags.create(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c", "nuggets"))).addTag(copperNuggets);
    }

    private void archetype(SulfurCubeArchetype archetype, String... ids) {
        var builder = tag(archetype.items());
        for (String id : ids) {
            if (id.startsWith("#")) {
                switch (id.substring(1)) {
                    case "minecraft:moss_blocks" -> builder.add(Items.MOSS_BLOCK, ModBlocks.PALE_MOSS_BLOCK.get().asItem());
                    case "minecraft:wart_blocks" -> builder.add(Items.NETHER_WART_BLOCK, Items.WARPED_WART_BLOCK);
                    case "minecraft:concrete" -> builder.addTag(net.neoforged.neoforge.common.Tags.Items.CONCRETES);
                    case "minecraft:concrete_powders" -> builder.addTag(net.neoforged.neoforge.common.Tags.Items.CONCRETE_POWDERS);
                    case "minecraft:glazed_terracotta" -> builder.addTag(net.neoforged.neoforge.common.Tags.Items.GLAZED_TERRACOTTAS);
                    default -> builder.addTag(ItemTags.create(net.minecraft.resources.ResourceLocation.parse(id.substring(1))));
                }
                continue;
            }
            var location = net.minecraft.resources.ResourceLocation.parse(id);
            if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(location)) {
                location = FutureBackport.id(location.getPath());
            }
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(location);
            if (item == Items.AIR) throw new IllegalStateException("Unknown sulfur cube block " + id);
            builder.add(item);
        }
    }
}
