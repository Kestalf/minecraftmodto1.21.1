package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModItems;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
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
}
