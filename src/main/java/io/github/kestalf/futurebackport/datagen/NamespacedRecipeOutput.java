package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * Vanilla recipe helpers build ids like {@code "<result>_from_<ingredient>_stonecutting"} which end up in the
 * {@code minecraft} namespace; this output moves them into the mod's namespace.
 */
record NamespacedRecipeOutput(RecipeOutput delegate) implements RecipeOutput {
    private static ResourceLocation fix(ResourceLocation id) {
        return id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) ? FutureBackport.id(id.getPath()) : id;
    }

    @Override
    public void accept(ResourceLocation id, Recipe<?> recipe, AdvancementHolder advancement, ICondition... conditions) {
        AdvancementHolder fixedAdvancement = advancement == null ? null : new AdvancementHolder(fix(advancement.id()), advancement.value());
        delegate.accept(fix(id), recipe, fixedAdvancement, conditions);
    }

    @Override
    public Advancement.Builder advancement() {
        return delegate.advancement();
    }
}
