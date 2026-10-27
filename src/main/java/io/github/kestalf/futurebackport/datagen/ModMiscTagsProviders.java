package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.registry.ModKeys;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BannerPatternTagsProvider;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.data.tags.PaintingVariantTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.PaintingVariantTags;
import net.minecraft.world.level.block.entity.BannerPatterns;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/** Tags of small data-driven registries. */
public final class ModMiscTagsProviders {
    public static class Enchantments extends EnchantmentTagsProvider {
        public Enchantments(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, FutureBackport.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(EnchantmentTags.IN_ENCHANTING_TABLE).add(ModKeys.LUNGE);
            tag(EnchantmentTags.NON_TREASURE).add(ModKeys.LUNGE);
            tag(EnchantmentTags.TRADEABLE).add(ModKeys.LUNGE);
            tag(EnchantmentTags.ON_RANDOM_LOOT).add(ModKeys.LUNGE);
            tag(EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT).add(ModKeys.LUNGE);
            tag(EnchantmentTags.ON_TRADED_EQUIPMENT).add(ModKeys.LUNGE);
        }
    }

    public static class Banners extends BannerPatternTagsProvider {
        public Banners(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, FutureBackport.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ModTags.BannerPatterns.FIELD_MASONED).add(BannerPatterns.BRICKS);
            tag(ModTags.BannerPatterns.BORDURE_INDENTED).add(BannerPatterns.CURLY_BORDER);
        }
    }

    public static class Paintings extends PaintingVariantTagsProvider {
        public Paintings(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, FutureBackport.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(PaintingVariantTags.PLACEABLE).add(ModKeys.DENNIS);
        }
    }

    private ModMiscTagsProviders() {
    }
}
