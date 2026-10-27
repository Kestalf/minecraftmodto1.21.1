package io.github.kestalf.futurebackport.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** Particles of the golden dandelion: sink down when growth is paused, rise when it is resumed. */
public class GrowthParticle extends TextureSheetParticle {
    protected GrowthParticle(ClientLevel level, double x, double y, double z, double ySpeed, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        this.xd = 0.0;
        this.zd = 0.0;
        this.yd = ySpeed;
        this.gravity = 0.0F;
        this.hasPhysics = false;
        this.lifetime = 16 + random.nextInt(8);
        this.pickSprite(sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet sprites, double ySpeed) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            return new GrowthParticle(level, x, y, z, ySpeed, sprites);
        }
    }
}
