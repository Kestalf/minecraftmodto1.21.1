package io.github.kestalf.futurebackport.entity.desert;

import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;

/**
 * Desert skeleton: less health, does not burn in daylight, shoots arrows of Weakness more slowly and is immune to
 * Weakness itself.
 */
public class Parched extends AbstractSkeleton {
    public Parched(EntityType<? extends Parched> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractSkeleton.createAttributes().add(Attributes.MAX_HEALTH, 16.0);
    }

    public static boolean checkSpawnRules(EntityType<Parched> type, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos,
                                          RandomSource random) {
        return Monster.checkMonsterSpawnRules(type, level, spawnType, pos, random)
                && (spawnType == MobSpawnType.SPAWNER || level.canSeeSky(pos));
    }

    @Override
    protected boolean isSunBurnTick() {
        return false;
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return !effect.is(MobEffects.WEAKNESS) && super.canBeAffected(effect);
    }

    @Override
    protected int getHardAttackInterval() {
        return 70;
    }

    @Override
    protected int getAttackInterval() {
        return 90;
    }

    @Override
    protected AbstractArrow getArrow(ItemStack arrow, float velocity, @Nullable ItemStack weapon) {
        AbstractArrow projectile = super.getArrow(arrow, velocity, weapon);
        // Like a tipped arrow of long Weakness: an eighth of the potion's four minutes.
        if (projectile instanceof Arrow tipped) tipped.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
        return projectile;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.event("entity.parched.ambient").get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.event("entity.parched.hurt").get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.event("entity.parched.death").get();
    }

    @Override
    protected SoundEvent getStepSound() {
        return ModSounds.event("entity.parched.step").get();
    }
}
