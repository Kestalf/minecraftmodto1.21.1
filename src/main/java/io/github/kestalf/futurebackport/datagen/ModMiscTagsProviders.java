package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.registry.ModKeys;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BannerPatternTagsProvider;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import io.github.kestalf.futurebackport.registry.ModEntities;
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

    public static class Entities extends EntityTypeTagsProvider {
        public Entities(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, FutureBackport.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            var nautilus = ModEntities.NAUTILUS.get();
            var zombieNautilus = ModEntities.ZOMBIE_NAUTILUS.get();
            tag(EntityTypeTags.AQUATIC).add(nautilus, zombieNautilus);
            tag(EntityTypeTags.CAN_BREATHE_UNDER_WATER).add(nautilus);
            tag(EntityTypeTags.ZOMBIES).add(zombieNautilus);
            tag(EntityTypeTags.NOT_SCARY_FOR_PUFFERFISH).add(nautilus, zombieNautilus);
            tag(EntityTypeTags.FALL_DAMAGE_IMMUNE).add(ModEntities.COPPER_GOLEM.get());
            tag(EntityTypeTags.UNDEAD).add(ModEntities.CAMEL_HUSK.get());
            tag(EntityTypeTags.SKELETONS).add(ModEntities.PARCHED.get());
        }
    }

    /** Biome tags of 1.21.5 that pick the warm and cold farm animal variants (the 26.3 lists). */
    public static class Biomes extends net.minecraft.data.tags.BiomeTagsProvider {
        public Biomes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, FutureBackport.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            tag(ModTags.Biomes.SPAWNS_WARM_VARIANT_FARM_ANIMALS)
                    .add(net.minecraft.world.level.biome.Biomes.DESERT, net.minecraft.world.level.biome.Biomes.WARM_OCEAN,
                            net.minecraft.world.level.biome.Biomes.MANGROVE_SWAMP, net.minecraft.world.level.biome.Biomes.DEEP_LUKEWARM_OCEAN,
                            net.minecraft.world.level.biome.Biomes.LUKEWARM_OCEAN)
                    .addTag(net.minecraft.tags.BiomeTags.IS_JUNGLE).addTag(net.minecraft.tags.BiomeTags.IS_SAVANNA)
                    .addTag(net.minecraft.tags.BiomeTags.IS_NETHER).addTag(net.minecraft.tags.BiomeTags.IS_BADLANDS);
            tag(ModTags.Biomes.SPAWNS_COLD_VARIANT_FARM_ANIMALS)
                    .add(net.minecraft.world.level.biome.Biomes.SNOWY_PLAINS, net.minecraft.world.level.biome.Biomes.ICE_SPIKES,
                            net.minecraft.world.level.biome.Biomes.FROZEN_PEAKS, net.minecraft.world.level.biome.Biomes.JAGGED_PEAKS,
                            net.minecraft.world.level.biome.Biomes.SNOWY_SLOPES, net.minecraft.world.level.biome.Biomes.FROZEN_OCEAN,
                            net.minecraft.world.level.biome.Biomes.DEEP_FROZEN_OCEAN, net.minecraft.world.level.biome.Biomes.GROVE,
                            net.minecraft.world.level.biome.Biomes.DEEP_DARK, net.minecraft.world.level.biome.Biomes.FROZEN_RIVER,
                            net.minecraft.world.level.biome.Biomes.SNOWY_TAIGA, net.minecraft.world.level.biome.Biomes.SNOWY_BEACH,
                            net.minecraft.world.level.biome.Biomes.COLD_OCEAN, net.minecraft.world.level.biome.Biomes.DEEP_COLD_OCEAN,
                            net.minecraft.world.level.biome.Biomes.OLD_GROWTH_PINE_TAIGA, net.minecraft.world.level.biome.Biomes.OLD_GROWTH_SPRUCE_TAIGA,
                            net.minecraft.world.level.biome.Biomes.TAIGA, net.minecraft.world.level.biome.Biomes.WINDSWEPT_FOREST,
                            net.minecraft.world.level.biome.Biomes.WINDSWEPT_GRAVELLY_HILLS, net.minecraft.world.level.biome.Biomes.WINDSWEPT_HILLS,
                            net.minecraft.world.level.biome.Biomes.STONY_PEAKS)
                    .addTag(net.minecraft.tags.BiomeTags.IS_END);
        }
    }

    private ModMiscTagsProviders() {
    }
}
