package io.github.kestalf.futurebackport.entity.ghast;

import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Ghastlings follow nearby players (preferred) or grown-up friendly mobs within 16 blocks, slightly faster than usual,
 * stopping when they are 5 blocks away.
 */
public class GhastlingFollowGoal extends Goal {
    private static final double RANGE = 16.0;
    private static final double STOP_DISTANCE = 5.0;
    private final HappyGhast ghastling;
    private LivingEntity target;
    private int recalculate;

    public GhastlingFollowGoal(HappyGhast ghastling) {
        this.ghastling = ghastling;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!ghastling.isBaby() || ghastling.getRandom().nextInt(80) != 0) return false;
        Player player = ghastling.level().getNearestPlayer(ghastling, RANGE);
        if (player != null && !player.isSpectator()) {
            target = player;
            return true;
        }
        List<LivingEntity> mobs = ghastling.level().getEntitiesOfClass(LivingEntity.class, ghastling.getBoundingBox().inflate(RANGE),
                e -> e != ghastling && e.isAlive() && !e.isBaby() && e.getType().is(ModTags.Entities.FOLLOWABLE_BY_GHASTLING));
        target = mobs.stream().min(Comparator.comparingDouble(ghastling::distanceToSqr)).orElse(null);
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && ghastling.isBaby() && ghastling.distanceToSqr(target) < RANGE * RANGE * 1.5;
    }

    @Override
    public void tick() {
        ghastling.getLookControl().setLookAt(target, 10.0F, ghastling.getMaxHeadXRot());
        if (--recalculate > 0) return;
        recalculate = 10;
        if (ghastling.distanceToSqr(target) > STOP_DISTANCE * STOP_DISTANCE) {
            ghastling.getNavigation().moveTo(target.getX(), target.getY() + 1.0, target.getZ(), 1.1);
        } else {
            ghastling.getNavigation().stop();
        }
    }

    @Override
    public void stop() {
        target = null;
        ghastling.getNavigation().stop();
    }
}
