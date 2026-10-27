package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.decoration.PaintingVariant;

/** Keys of data-driven registry entries defined by the mod (generated in datagen). */
public final class ModKeys {
    public static final ResourceKey<Enchantment> LUNGE = ResourceKey.create(Registries.ENCHANTMENT, FutureBackport.id("lunge"));
    public static final ResourceKey<DamageType> SPEAR = ResourceKey.create(Registries.DAMAGE_TYPE, FutureBackport.id("spear"));
    public static final ResourceKey<JukeboxSong> TEARS = ResourceKey.create(Registries.JUKEBOX_SONG, FutureBackport.id("tears"));
    public static final ResourceKey<JukeboxSong> LAVA_CHICKEN = ResourceKey.create(Registries.JUKEBOX_SONG, FutureBackport.id("lava_chicken"));
    public static final ResourceKey<JukeboxSong> BOUNCE = ResourceKey.create(Registries.JUKEBOX_SONG, FutureBackport.id("bounce"));
    public static final ResourceKey<PaintingVariant> DENNIS = ResourceKey.create(Registries.PAINTING_VARIANT, FutureBackport.id("dennis"));

    private ModKeys() {
    }
}
