package io.github.kestalf.futurebackport.entity.sulfur;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * How a sulfur cube behaves with a block inside, chosen by the item tag {@code futurebackport:sulfur_cube_archetype/<name>}
 * the block belongs to. The numbers are those of the 26.3 archetype definitions: the knockback resistance applies to
 * hits and explosions (negative means extra knockback), friction and air drag scale how much speed is lost on the
 * ground and in the air, bounciness is the share of speed kept when bouncing off a block.
 */
public enum SulfurCubeArchetype {
    BOUNCY("bouncy", -2.0F, 0.9F, 0.3F, 0.01F, 0.4125F, 0.105F, true, 0.7F, 0.3F),
    EXPLOSIVE("explosive", -1.0F, 0.5F, 0.3F, 0.3F, 0.4125F, 0.09F, true, 0.7F, 0.1F),
    FAST_FLAT("fast_flat", -1.0F, 0.5F, 0.2F, 0.01F, 0.9125F, 0.09F, false, 0.9F, 0.03F),
    FAST_SLIDING("fast_sliding", 0.5F, 0.1F, 0.05F, 0.01F, 0.6625F, 0.09F, false, 1.0F, 0.05F),
    HIGH_RESISTANCE("high_resistance", 0.7F, 0.2F, 1.0F, 0.01F, 0.4125F, 0.09F, false, 0.7F, 0.03F),
    HOT("hot", -1.0F, 0.5F, 0.3F, 0.1F, 0.4125F, 0.09F, true, 0.7F, 0.2F),
    LIGHT("light", -1.0F, 1.0F, 0.3F, 1.8F, 0.4125F, 0.18F, true, 0.7F, 0.2F),
    REGULAR("regular", -1.0F, 0.5F, 0.3F, 0.1F, 0.4125F, 0.09F, true, 0.5F, 0.2F),
    SLOW_BOUNCY("slow_bouncy", 0.4F, 0.6F, 0.3F, 0.05F, 0.4125F, 0.24F, false, 0.5F, 0.05F),
    SLOW_FLAT("slow_flat", 0.5F, 0.4F, 0.4F, 0.1F, 0.4125F, 0.105F, false, 0.9F, 0.03F),
    SLOW_SLIDING("slow_sliding", 0.8F, 0.1F, 0.05F, 0.01F, 0.4125F, 0.09F, false, 1.0F, 0.02F),
    STICKY("sticky", -2.0F, 0.0F, 2.0F, 0.01F, 0.4125F, 0.09F, false, 0.5F, 0.05F);

    /** Fuse of a manually lit explosive cube and the power of its explosion. */
    public static final int FUSE = 120;
    public static final float EXPLOSION_POWER = 3.0F;
    /** Damage a hot cube deals to whatever touches it, every half second. */
    public static final float CONTACT_DAMAGE = 1.0F;

    private final String name;
    private final TagKey<Item> items;
    public final float knockbackResistance;
    public final float bounciness;
    public final float friction;
    public final float airDrag;
    public final float horizontalPower;
    public final float verticalPower;
    public final boolean buoyant;
    /** Seconds between two push sounds, and the speed a push has to reach to make one. */
    public final float pushSoundCooldown;
    public final float pushSoundThreshold;

    SulfurCubeArchetype(String name, float knockbackResistance, float bounciness, float friction, float airDrag, float horizontalPower,
                        float verticalPower, boolean buoyant, float pushSoundCooldown, float pushSoundThreshold) {
        this.name = name;
        this.items = TagKey.create(Registries.ITEM, FutureBackport.id("sulfur_cube_archetype/" + name));
        this.knockbackResistance = knockbackResistance;
        this.bounciness = bounciness;
        this.friction = friction;
        this.airDrag = airDrag;
        this.horizontalPower = horizontalPower;
        this.verticalPower = verticalPower;
        this.buoyant = buoyant;
        this.pushSoundCooldown = pushSoundCooldown;
        this.pushSoundThreshold = pushSoundThreshold;
    }

    public String getName() {
        return name;
    }

    public TagKey<Item> items() {
        return items;
    }

    public SoundEvent hitSound() {
        return ModSounds.event("entity.sulfur_cube." + name + ".hit").get();
    }

    public SoundEvent pushSound() {
        return ModSounds.event("entity.sulfur_cube." + name + ".push").get();
    }

    /** The archetype the stack gives a cube, or null if a cube cannot absorb it. */
    @Nullable
    public static SulfurCubeArchetype of(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (SulfurCubeArchetype archetype : values()) {
            if (stack.is(archetype.items)) return archetype;
        }
        return null;
    }
}
