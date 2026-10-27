package io.github.kestalf.futurebackport.client.particle;

import io.github.kestalf.futurebackport.particle.TrailParticleOption;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A full-bright dot that slides to its target position (the trails between a creaking and its heart). */
public class TrailParticle extends TextureSheetParticle {
    private final Vec3 target;

    TrailParticle(ClientLevel level, double x, double y, double z, Vec3 target, int color) {
        super(level, x, y, z, 0.0, 0.0, 0.0);
        float shade = 0.875F + random.nextFloat() * 0.25F;
        this.rCol = Math.min(1.0F, ((color >> 16) & 0xFF) / 255.0F * shade);
        this.gCol = Math.min(1.0F, ((color >> 8) & 0xFF) / 255.0F * shade);
        this.bCol = Math.min(1.0F, (color & 0xFF) / 255.0F * shade);
        this.quadSize = 0.26F;
        this.target = target;
        this.hasPhysics = false;
        this.gravity = 0.0F;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        double step = 1.0 / (lifetime - age + 1);
        x = Mth.lerp(step, x, target.x);
        y = Mth.lerp(step, y, target.y);
        z = Mth.lerp(step, z, target.z);
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<TrailParticleOption> {
        @Override
        public Particle createParticle(TrailParticleOption option, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            TrailParticle particle = new TrailParticle(level, x, y, z, option.target(), option.color());
            particle.pickSprite(sprites);
            particle.setLifetime(option.duration());
            return particle;
        }
    }
}
