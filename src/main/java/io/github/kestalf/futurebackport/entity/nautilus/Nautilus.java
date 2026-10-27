package io.github.kestalf.futurebackport.entity.nautilus;

import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** The nautilus of the ocean; breeds with fish and has its own baby sounds. */
public class Nautilus extends AbstractNautilus {
    public Nautilus(EntityType<? extends Nautilus> type, Level level) {
        super(type, level);
    }

    @Override
    protected SoundEvent sound(String name) {
        boolean babySound = isBaby() && !name.startsWith("dash") && !name.equals("riding");
        return ModSounds.event((babySound ? "entity.baby_nautilus." : "entity.nautilus.") + name).get();
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Nautilus baby = ModEntities.NAUTILUS.get().create(level);
        if (baby != null && isTame()) {
            java.util.UUID owner = getOwnerUUID();
            if (owner != null) {
                baby.setOwnerUUID(owner);
                baby.setTame(true, true);
            }
        }
        return baby;
    }
}
