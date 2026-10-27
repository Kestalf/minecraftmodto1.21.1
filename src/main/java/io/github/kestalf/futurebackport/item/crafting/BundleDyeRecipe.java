package io.github.kestalf.futurebackport.item.crafting;

import io.github.kestalf.futurebackport.registry.ModItems;
import io.github.kestalf.futurebackport.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Bundle + dye = dyed bundle that keeps its contents (the "crafting_transmute" recipe of 1.21.2+). */
public class BundleDyeRecipe extends CustomRecipe {
    public BundleDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    private record Parts(ItemStack bundle, DyeItem dye) {
    }

    private static Parts find(CraftingInput input) {
        ItemStack bundle = ItemStack.EMPTY;
        DyeItem dye = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() instanceof BundleItem && bundle.isEmpty()) bundle = stack;
            else if (stack.getItem() instanceof DyeItem d && dye == null) dye = d;
            else return null;
        }
        return bundle.isEmpty() || dye == null ? null : new Parts(bundle, dye);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        Parts parts = find(input);
        return parts != null && !parts.bundle().is(ModItems.BUNDLES.get(parts.dye().getDyeColor()).get());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Parts parts = find(input);
        if (parts == null) return ItemStack.EMPTY;
        return parts.bundle().transmuteCopy(ModItems.BUNDLES.get(parts.dye().getDyeColor()).get(), 1);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BUNDLE_DYE.get();
    }
}
