package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Particle types. The simple types are created eagerly because blocks (e.g. the copper torch) need the instance while
 * the block registry is being filled, which happens before particle types are registered.
 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, FutureBackport.MOD_ID);

    public static final SimpleParticleType COPPER_FIRE_FLAME = simple("copper_fire_flame", false);
    public static final SimpleParticleType PALE_OAK_LEAVES = simple("pale_oak_leaves", false);
    public static final SimpleParticleType RED_POPLAR_LEAVES = simple("red_poplar_leaves", false);
    public static final SimpleParticleType ORANGE_POPLAR_LEAVES = simple("orange_poplar_leaves", false);
    public static final SimpleParticleType YELLOW_POPLAR_LEAVES = simple("yellow_poplar_leaves", false);
    public static final SimpleParticleType FIREFLY = simple("firefly", false);
    public static final SimpleParticleType PAUSE_MOB_GROWTH = simple("pause_mob_growth", false);
    public static final SimpleParticleType RESET_MOB_GROWTH = simple("reset_mob_growth", false);
    public static final SimpleParticleType NOXIOUS_GAS = simple("noxious_gas", false);
    public static final SimpleParticleType SULFUR_BUBBLES = simple("sulfur_bubbles", false);
    public static final SimpleParticleType GEYSER_BASE = simple("geyser_base", true);
    public static final SimpleParticleType GEYSER_PLUME = simple("geyser_plume", true);
    public static final SimpleParticleType GEYSER_POOF = simple("geyser_poof", true);
    public static final SimpleParticleType SULFUR_CUBE_GOO = simple("sulfur_cube_goo", false);

    private static SimpleParticleType simple(String name, boolean overrideLimiter) {
        SimpleParticleType type = new SimpleParticleType(overrideLimiter);
        PARTICLES.register(name, () -> type);
        return type;
    }

    public static DeferredHolder<ParticleType<?>, ParticleType<?>> holder(String name) {
        return DeferredHolder.create(Registries.PARTICLE_TYPE, FutureBackport.id(name));
    }

    private ModParticles() {
    }
}
