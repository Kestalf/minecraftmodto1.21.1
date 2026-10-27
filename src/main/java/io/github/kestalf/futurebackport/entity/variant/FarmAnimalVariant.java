package io.github.kestalf.futurebackport.entity.variant;

import com.mojang.serialization.Codec;
import io.github.kestalf.futurebackport.registry.ModAttachments;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.LevelReader;

/** The temperate, warm and cold variants of chickens, cows and pigs (1.21.5). */
public enum FarmAnimalVariant implements StringRepresentable {
    TEMPERATE("temperate"),
    WARM("warm"),
    COLD("cold");

    public static final Codec<FarmAnimalVariant> CODEC = StringRepresentable.fromEnum(FarmAnimalVariant::values);
    private final String name;

    FarmAnimalVariant(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static boolean hasVariants(Entity entity) {
        return entity instanceof Chicken || entity instanceof Pig || entity instanceof Cow && !(entity instanceof MushroomCow);
    }

    public static FarmAnimalVariant of(Entity entity) {
        return entity.getExistingData(ModAttachments.FARM_VARIANT).orElse(TEMPERATE);
    }

    /** The variant that spawns at {@code pos}, from the biome tags of 1.21.5. */
    public static FarmAnimalVariant forBiome(LevelReader level, BlockPos pos) {
        var biome = level.getBiome(pos);
        if (biome.is(ModTags.Biomes.SPAWNS_WARM_VARIANT_FARM_ANIMALS)) return WARM;
        if (biome.is(ModTags.Biomes.SPAWNS_COLD_VARIANT_FARM_ANIMALS)) return COLD;
        return TEMPERATE;
    }
}
