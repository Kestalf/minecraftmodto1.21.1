package io.github.kestalf.futurebackport.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** A glowing firefly that wanders around slowly, fading in and out. */
public class FireflyParticle extends TextureSheetParticle {
    protected FireflyParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.speedUpWhenYMotionIsBlocked = true;
        this.friction = 0.96F;
        this.quadSize *= 0.75F;
        this.xd = (random.nextDouble() - 0.5) * 0.05;
        this.yd = (random.nextDouble() - 0.5) * 0.05;
        this.zd = (random.nextDouble() - 0.5) * 0.05;
        this.lifetime = 200 + random.nextInt(100);
        this.alpha = 0.0F;
        this.pickSprite(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (random.nextInt(10) == 0) {
            this.xd += (random.nextDouble() - 0.5) * 0.02;
            this.yd += (random.nextDouble() - 0.5) * 0.02;
            this.zd += (random.nextDouble() - 0.5) * 0.02;
        }
        float progress = (float) age / lifetime;
        // Fade in during the first 30% and out during the last 50% of the lifetime, with a gentle flicker.
        float fade = progress < 0.3F ? progress / 0.3F : progress > 0.5F ? (1.0F - progress) / 0.5F : 1.0F;
        this.alpha = Mth.clamp(fade * (0.8F + 0.2F * Mth.sin(age * 0.3F)), 0.0F, 1.0F);
    }

    @Override
    protected int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new FireflyParticle(level, x, y, z, sprites);
        }
    }
}
