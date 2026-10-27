package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.block.CopperFamily;
import io.github.kestalf.futurebackport.block.StoneFamily;
import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModItems;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput rawOutput) {
        RecipeOutput output = new NamespacedRecipeOutput(rawOutput);
        wood(output, ModBlocks.PALE_OAK, ModTags.Items.PALE_OAK_LOGS);
        wood(output, ModBlocks.POPLAR, ModTags.Items.POPLAR_LOGS);
        minerals(output);
        resin(output);
        coloredStairsAndSlabs(output);
        copper(output);
        plants(output);
    }

    private void wood(RecipeOutput output, WoodFamily f, net.minecraft.tags.TagKey<net.minecraft.world.item.Item> logs) {
        planksFromLogs(output, f.planks.get(), logs, 4);
        woodFromLogs(output, f.wood.get(), f.log.get());
        woodFromLogs(output, f.strippedWood.get(), f.strippedLog.get());
        BlockFamily family = new BlockFamily.Builder(f.planks.get())
                .button(f.button.get()).fence(f.fence.get()).fenceGate(f.fenceGate.get()).pressurePlate(f.pressurePlate.get())
                .sign(f.sign.get(), f.wallSign.get()).slab(f.slab.get()).stairs(f.stairs.get()).door(f.door.get())
                .trapdoor(f.trapdoor.get()).recipeGroupPrefix("wooden").recipeUnlockedBy("has_planks").getFamily();
        generateRecipes(output, family, FeatureFlags.VANILLA_SET);
        hangingSign(output, f.hangingSignItem.get(), f.strippedLog.get());
        woodenBoat(output, f.boat.get(), f.planks.get());
        chestBoat(output, f.chestBoat.get(), f.boat.get());
    }

    private void stoneFamily(RecipeOutput output, StoneFamily family, Block chiseled) {
        Block base = family.base().get();
        stairBuilder(family.stairs().get(), Ingredient.of(base)).unlockedBy(getHasName(base), has(base)).save(output);
        slab(output, RecipeCategory.BUILDING_BLOCKS, family.slab().get(), base);
        wall(output, RecipeCategory.DECORATIONS, family.wall().get(), base);
        if (chiseled != null) chiseled(output, RecipeCategory.BUILDING_BLOCKS, chiseled, family.slab().get());
    }

    /** All stonecutter recipes of a chain of families: each family can be cut into its own and all later variants. */
    private void stonecutting(RecipeOutput output, List<StoneFamily> chain, List<Block> chiseled) {
        for (int i = 0; i < chain.size(); i++) {
            Block from = chain.get(i).base().get();
            for (int j = i; j < chain.size(); j++) {
                StoneFamily to = chain.get(j);
                if (j > i) stonecutterResultFromBase(output, RecipeCategory.BUILDING_BLOCKS, to.base().get(), from);
                stonecutterResultFromBase(output, RecipeCategory.BUILDING_BLOCKS, to.stairs().get(), from);
                stonecutterResultFromBase(output, RecipeCategory.BUILDING_BLOCKS, to.slab().get(), from, 2);
                stonecutterResultFromBase(output, RecipeCategory.DECORATIONS, to.wall().get(), from);
            }
            for (Block c : chiseled) stonecutterResultFromBase(output, RecipeCategory.BUILDING_BLOCKS, c, from);
        }
    }

    private void minerals(RecipeOutput output) {
        stoneFamily(output, ModBlocks.CINNABAR, null);
        stoneFamily(output, ModBlocks.POLISHED_CINNABAR, null);
        stoneFamily(output, ModBlocks.CINNABAR_BRICKS, ModBlocks.CHISELED_CINNABAR.get());
        polished(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.POLISHED_CINNABAR.base().get(), ModBlocks.CINNABAR.base().get());
        polished(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CINNABAR_BRICKS.base().get(), ModBlocks.POLISHED_CINNABAR.base().get());
        stonecutting(output, List.of(ModBlocks.CINNABAR, ModBlocks.POLISHED_CINNABAR, ModBlocks.CINNABAR_BRICKS),
                List.of(ModBlocks.CHISELED_CINNABAR.get()));

        stoneFamily(output, ModBlocks.SULFUR, null);
        stoneFamily(output, ModBlocks.POLISHED_SULFUR, null);
        stoneFamily(output, ModBlocks.SULFUR_BRICKS, ModBlocks.CHISELED_SULFUR.get());
        polished(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.POLISHED_SULFUR.base().get(), ModBlocks.SULFUR.base().get());
        polished(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.SULFUR_BRICKS.base().get(), ModBlocks.POLISHED_SULFUR.base().get());
        stonecutting(output, List.of(ModBlocks.SULFUR, ModBlocks.POLISHED_SULFUR, ModBlocks.SULFUR_BRICKS),
                List.of(ModBlocks.CHISELED_SULFUR.get()));
    }

    private void resin(RecipeOutput output) {
        nineBlockStorageRecipes(output, RecipeCategory.MISC, ModBlocks.RESIN_CLUMP.get(), RecipeCategory.BUILDING_BLOCKS, ModBlocks.RESIN_BLOCK.get());
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ModBlocks.RESIN_CLUMP.get()), RecipeCategory.MISC, ModItems.RESIN_BRICK.get(), 0.1F, 200)
                .unlockedBy(getHasName(ModBlocks.RESIN_CLUMP.get()), has(ModBlocks.RESIN_CLUMP.get())).save(output);
        twoByTwoPacker(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.RESIN_BRICKS.base().get(), ModItems.RESIN_BRICK.get());
        stoneFamily(output, ModBlocks.RESIN_BRICKS, ModBlocks.CHISELED_RESIN_BRICKS.get());
        stonecutting(output, List.of(ModBlocks.RESIN_BRICKS), List.of(ModBlocks.CHISELED_RESIN_BRICKS.get()));
    }

    private void coloredStairsAndSlabs(RecipeOutput output) {
        for (DyeColor color : DyeColor.values()) {
            Block wool = ModBlocks.VanillaColored.wool(color);
            Block concrete = ModBlocks.VanillaColored.concrete(color);
            stairBuilder(ModBlocks.WOOL_STAIRS.get(color).get(), Ingredient.of(wool)).group("wool_stairs")
                    .unlockedBy(getHasName(wool), has(wool)).save(output);
            slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.WOOL_SLABS.get(color).get(), Ingredient.of(wool)).group("wool_slab")
                    .unlockedBy(getHasName(wool), has(wool)).save(output);
            stairBuilder(ModBlocks.CONCRETE_STAIRS.get(color).get(), Ingredient.of(concrete)).group("concrete_stairs")
                    .unlockedBy(getHasName(concrete), has(concrete)).save(output);
            slabBuilder(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_SLABS.get(color).get(), Ingredient.of(concrete)).group("concrete_slab")
                    .unlockedBy(getHasName(concrete), has(concrete)).save(output);
            stonecutterResultFromBase(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_STAIRS.get(color).get(), concrete);
            stonecutterResultFromBase(output, RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_SLABS.get(color).get(), concrete, 2);
        }
    }

    private void copper(RecipeOutput output) {
        nineBlockStorageRecipesRecipesWithCustomUnpacking(output, RecipeCategory.MISC, ModItems.COPPER_NUGGET.get(), RecipeCategory.MISC,
                Items.COPPER_INGOT, "copper_ingot_from_nuggets", "copper_ingot");
        CopperFamily copper = ModBlocks.COPPER;
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, copper.bars.unwaxed.get(WeatherState.UNAFFECTED).get(), 16)
                .define('#', Items.COPPER_INGOT).pattern("###").pattern("###")
                .unlockedBy("has_copper_ingot", has(Items.COPPER_INGOT)).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, copper.chains.unwaxed.get(WeatherState.UNAFFECTED).get())
                .define('I', Items.COPPER_INGOT).define('N', ModItems.COPPER_NUGGET.get()).pattern("N").pattern("I").pattern("N")
                .unlockedBy("has_copper_nugget", has(ModItems.COPPER_NUGGET.get())).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, copper.torchItem.get(), 4)
                .define('N', ModItems.COPPER_NUGGET.get()).define('C', ItemTags.COALS).define('S', Items.STICK)
                .pattern("N").pattern("C").pattern("S")
                .unlockedBy("has_copper_nugget", has(ModItems.COPPER_NUGGET.get())).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, copper.lanterns.unwaxed.get(WeatherState.UNAFFECTED).get())
                .define('N', ModItems.COPPER_NUGGET.get()).define('T', copper.torchItem.get())
                .pattern("NNN").pattern("NTN").pattern("NNN")
                .unlockedBy("has_copper_torch", has(copper.torchItem.get())).save(output);
        for (CopperFamily.Weathering<?> weathering : List.of(copper.bars, copper.chains, copper.lanterns, copper.lightningRods)) {
            for (WeatherState state : WeatherState.values()) {
                ItemLike unwaxed = weathering.unwaxed.containsKey(state) ? weathering.unwaxed.get(state).get() : Items.LIGHTNING_ROD;
                ItemLike waxed = weathering.waxed.get(state).get();
                ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, waxed).requires(unwaxed).requires(Items.HONEYCOMB)
                        .group(getItemName(waxed)).unlockedBy(getHasName(unwaxed), has(unwaxed))
                        .save(output, FutureBackport.id(getConversionRecipeName(waxed, Items.HONEYCOMB)));
            }
        }
    }

    private void plants(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.PALE_MOSS_CARPET.get(), 3)
                .define('#', ModBlocks.PALE_MOSS_BLOCK.get()).pattern("##")
                .unlockedBy(getHasName(ModBlocks.PALE_MOSS_BLOCK.get()), has(ModBlocks.PALE_MOSS_BLOCK.get())).save(output);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.GOLDEN_DANDELION.get())
                .define('G', Items.GOLD_NUGGET).define('D', Items.DANDELION).pattern("GGG").pattern("GDG").pattern("GGG")
                .unlockedBy("has_dandelion", has(Items.DANDELION)).save(output);
        dye(output, Items.YELLOW_DYE, ModBlocks.GOLDEN_DANDELION.get(), 1);
        dye(output, Items.YELLOW_DYE, ModBlocks.WILDFLOWERS.get(), 1);
        dye(output, Items.PINK_DYE, ModBlocks.CACTUS_FLOWER.get(), 1);
        dye(output, Items.ORANGE_DYE, ModBlocks.OPEN_EYEBLOSSOM.get(), 1);
        dye(output, Items.GRAY_DYE, ModBlocks.CLOSED_EYEBLOSSOM.get(), 1);
        dye(output, Items.RED_DYE, ModBlocks.RED_SHRUB.get(), 1);
    }

    private void dye(RecipeOutput output, ItemLike dye, ItemLike flower, int count) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, dye, count).requires(flower).group(getItemName(dye))
                .unlockedBy(getHasName(flower), has(flower)).save(output, FutureBackport.id(getConversionRecipeName(dye, flower)));
    }
}
