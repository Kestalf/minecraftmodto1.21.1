package io.github.kestalf.futurebackport.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.BlockPos;

/** Particles of potent sulfur and sulfur cubes: animated sprites with a few motion presets. */
public final class SulfurParticles {
    /** How a particle moves and how long it lives. */
    public record Style(int minLife, int maxLife, float size, float gravity, float friction, boolean onlyInWater, boolean fade) {
        public static final Style GEYSER_BASE = new Style(8, 12, 1.4F, 0.0F, 0.9F, false, false);
        public static final Style GEYSER_PLUME = new Style(20, 30, 0.9F, 0.4F, 0.98F, false, true);
        public static final Style GEYSER_POOF = new Style(16, 24, 1.2F, -0.01F, 0.9F, false, true);
        public static final Style NOXIOUS_GAS = new Style(60, 100, 0.7F, -0.002F, 0.96F, false, true);
        public static final Style SULFUR_BUBBLES = new Style(20, 40, 0.2F, -0.02F, 0.85F, true, false);
        public static final Style GOO = new Style(16, 28, 0.25F, 0.6F, 0.98F, false, false);
    }

    private static final class Animated extends TextureSheetParticle {
        private final SpriteSet sprites;
        private final Style style;

        Animated(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, Style style) {
            super(level, x, y, z, 0.0, 0.0, 0.0);
            this.sprites = sprites;
            this.style = style;
            this.xd = dx;
            this.yd = dy;
            this.zd = dz;
            this.gravity = style.gravity();
            this.friction = style.friction();
            this.hasPhysics = false;
            this.lifetime = style.minLife() + random.nextInt(style.maxLife() - style.minLife() + 1);
            this.quadSize *= style.size() * 5.0F;
            setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (removed) return;
            setSpriteFromAge(sprites);
            if (style.fade()) alpha = 1.0F - (float) age / lifetime;
            if (style.onlyInWater() && !level.getFluidState(BlockPos.containing(x, y, z)).is(FluidTags.WATER)) remove();
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    public record Provider(SpriteSet sprites, Style style) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new Animated(level, x, y, z, dx, dy, dz, sprites, style);
        }
    }

    private SulfurParticles() {
    }
}
