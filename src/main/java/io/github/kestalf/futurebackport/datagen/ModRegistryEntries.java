package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModKeys;
import io.github.kestalf.futurebackport.registry.ModSounds;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.enchantment.Enchantment;

/** Data-driven registry entries (enchantments, damage types, music disc songs, paintings). */
public final class ModRegistryEntries {
    public static void enchantments(BootstrapContext<Enchantment> context) {
        var items = context.lookup(Registries.ITEM);
        context.register(ModKeys.LUNGE, Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ModTags.Items.ENCHANTABLE_LUNGE), 5, 3,
                        Enchantment.dynamicCost(5, 8), Enchantment.dynamicCost(25, 8), 2, EquipmentSlotGroup.HAND))
                .build(ModKeys.LUNGE.location()));
    }

    public static void damageTypes(BootstrapContext<DamageType> context) {
        context.register(ModKeys.SPEAR, new DamageType("spear", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
        context.register(ModKeys.SULFUR_CUBE_HOT, new DamageType("sulfurCubeHot", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F,
                net.minecraft.world.damagesource.DamageEffects.BURNING));
    }

    public static void jukeboxSongs(BootstrapContext<JukeboxSong> context) {
        song(context, ModKeys.TEARS, 175.0F, 10);
        song(context, ModKeys.LAVA_CHICKEN, 134.0F, 9);
        song(context, ModKeys.BOUNCE, 234.0F, 8);
    }

    private static void song(BootstrapContext<JukeboxSong> context, ResourceKey<JukeboxSong> key, float length, int comparator) {
        String name = key.location().getPath();
        context.register(key, new JukeboxSong(ModSounds.event("music_disc." + name),
                Component.translatable("jukebox_song." + FutureBackport.MOD_ID + "." + name), length, comparator));
    }

    public static void paintings(BootstrapContext<PaintingVariant> context) {
        context.register(ModKeys.DENNIS, new PaintingVariant(3, 3, ModKeys.DENNIS.location()));
    }

    /** Natural spawns of the new mobs. */
    public static void biomeModifiers(BootstrapContext<net.neoforged.neoforge.common.world.BiomeModifier> context) {
        var biomes = context.lookup(Registries.BIOME);
        context.register(net.minecraft.resources.ResourceKey.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                FutureBackport.id("spawn_nautilus")), net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                biomes.getOrThrow(net.minecraft.tags.BiomeTags.IS_OCEAN),
                new net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData(ModEntities.NAUTILUS.get(), 2, 1, 3)));
        context.register(net.minecraft.resources.ResourceKey.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIERS,
                FutureBackport.id("spawn_parched")), net.neoforged.neoforge.common.world.BiomeModifiers.AddSpawnsBiomeModifier.singleSpawn(
                net.minecraft.core.HolderSet.direct(biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.DESERT)),
                new net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData(ModEntities.PARCHED.get(), 50, 4, 4)));
    }

    private ModRegistryEntries() {
    }
}
