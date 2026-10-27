package io.github.kestalf.futurebackport.registry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Sound events of the new content. The sound files and the sounds.json come from the official client (imported at
 * build time); every event listed in that sounds.json is registered automatically, the constants below are the ones
 * referenced from code.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, FutureBackport.MOD_ID);
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> BY_NAME = new HashMap<>();

    public static final SoundType RESIN_TYPE = blockSound("resin", 1.0F, 1.0F, "break", "step", "place", "break", "fall");
    public static final SoundType RESIN_BRICKS_TYPE = blockSound("resin_bricks");
    public static final SoundType CINNABAR_TYPE = blockSound("cinnabar");
    public static final SoundType SULFUR_TYPE = blockSound("sulfur");
    public static final SoundType POTENT_SULFUR_TYPE = blockSound("potent_sulfur");
    public static final SoundType SULFUR_SPIKE_TYPE = blockSound("sulfur_spike");
    public static final SoundType LEAF_LITTER_TYPE = blockSound("leaf_litter");
    public static final SoundType SHELF_TYPE = blockSound("shelf");
    public static final SoundType SHELF_MUSHROOM_TYPE = blockSound("shelf_mushroom", 1.0F, 1.0F, "break", "step", "place", "break", "fall");
    public static final SoundType STRAW_BED_TYPE = blockSound("straw_bed");
    public static final SoundType DRIED_GHAST_TYPE = blockSound("dried_ghast", 1.0F, 1.0F, "break", "step", "place", "break", "fall");
    public static final SoundType CREAKING_HEART_TYPE = blockSound("creaking_heart");
    public static final SoundType COPPER_GOLEM_STATUE_TYPE = blockSound("copper_golem_statue");
    public static final SoundType POPLAR_LEAVES_TYPE = blockSound("poplar_leaves");
    public static final SoundType IRON_TYPE = blockSound("iron");
    public static final SoundType CACTUS_FLOWER_TYPE = new DeferredSoundType(1.0F, 1.0F, event("block.cactus_flower.break"),
            () -> SoundEvents.GRASS_STEP, event("block.cactus_flower.place"), () -> SoundEvents.GRASS_HIT, () -> SoundEvents.GRASS_FALL);
    public static final SoundType RED_SHRUB_TYPE = new DeferredSoundType(1.0F, 1.0F, event("block.red_shrub.break"),
            () -> SoundEvents.GRASS_STEP, event("block.red_shrub.place"), () -> SoundEvents.GRASS_HIT, () -> SoundEvents.GRASS_FALL);

    private ModSounds() {
    }

    /** Returns (registering it if needed) the sound event {@code futurebackport:<name>}. */
    public static DeferredHolder<SoundEvent, SoundEvent> event(String name) {
        return BY_NAME.computeIfAbsent(name, n -> SOUNDS.register(n, () -> SoundEvent.createVariableRangeEvent(FutureBackport.id(n))));
    }

    private static SoundType blockSound(String block) {
        return blockSound(block, 1.0F, 1.0F, "break", "step", "place", "hit", "fall");
    }

    private static SoundType blockSound(String block, float volume, float pitch, String breakSound, String step, String place, String hit, String fall) {
        String prefix = "block." + block + ".";
        return new DeferredSoundType(volume, pitch, event(prefix + breakSound), event(prefix + step), event(prefix + place),
                event(prefix + hit), event(prefix + fall));
    }

    /** Registers every sound event listed in the imported sounds.json. Called from the mod constructor. */
    public static void registerImported() {
        Path path = ModList.get().getModFileById(FutureBackport.MOD_ID).getFile().findResource("assets", FutureBackport.MOD_ID, "sounds.json");
        if (!Files.exists(path)) {
            FutureBackport.LOGGER.warn("sounds.json is missing - did the vanilla asset import run?");
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject sounds = JsonParser.parseReader(reader).getAsJsonObject();
            sounds.keySet().forEach(ModSounds::event);
        } catch (IOException e) {
            FutureBackport.LOGGER.error("Could not read sounds.json", e);
        }
    }
}
