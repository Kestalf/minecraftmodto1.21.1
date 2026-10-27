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
            tag(EntityTypeTags.NOT_SCARY_FOR_PUFFERFISH).add(ModEntities.SULFUR_CUBE.get());
        }
    }

    public static class DamageTypes extends net.minecraft.data.tags.TagsProvider<net.minecraft.world.damagesource.DamageType> {
        public DamageTypes(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, net.minecraft.core.registries.Registries.DAMAGE_TYPE, lookup, FutureBackport.MOD_ID, helper);
        }

        @Override
        protected void addTags(HolderLookup.Provider provider) {
            // What a sulfur cube with a block shrugs off (26.3 list; mace smashes are player attacks here).
            tag(ModTags.DamageTypes.SULFUR_CUBE_WITH_BLOCK_IMMUNE_TO).add(net.minecraft.world.damagesource.DamageTypes.ARROW,
                    net.minecraft.world.damagesource.DamageTypes.CACTUS, net.minecraft.world.damagesource.DamageTypes.FALL,
                    net.minecraft.world.damagesource.DamageTypes.FALLING_ANVIL, net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK,
                    net.minecraft.world.damagesource.DamageTypes.FALLING_STALACTITE, net.minecraft.world.damagesource.DamageTypes.FREEZE,
                    net.minecraft.world.damagesource.DamageTypes.HOT_FLOOR, net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK,
                    net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK_NO_AGGRO, net.minecraft.world.damagesource.DamageTypes.MOB_PROJECTILE,
                    net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK, ModKeys.SPEAR, net.minecraft.world.damagesource.DamageTypes.SPIT,
                    net.minecraft.world.damagesource.DamageTypes.STALAGMITE, net.minecraft.world.damagesource.DamageTypes.STING,
                    ModKeys.SULFUR_CUBE_HOT, net.minecraft.world.damagesource.DamageTypes.SWEET_BERRY_BUSH,
                    net.minecraft.world.damagesource.DamageTypes.THROWN, net.minecraft.world.damagesource.DamageTypes.TRIDENT,
                    net.minecraft.world.damagesource.DamageTypes.WIND_CHARGE)
                    .addTag(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
            for (var tag : java.util.List.of(net.minecraft.tags.DamageTypeTags.IS_FIRE, net.minecraft.tags.DamageTypeTags.BURN_FROM_STEPPING,
                    net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK, net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD,
                    net.minecraft.tags.DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES)) {
                tag(tag).add(ModKeys.SULFUR_CUBE_HOT);
            }
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
