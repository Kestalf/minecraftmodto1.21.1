package io.github.kestalf.futurebackport.item.spear;

import io.github.kestalf.futurebackport.registry.ModKeys;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Server and shared logic of spear jabs, charge attacks and the Lunge enchantment. */
public final class SpearCombat {
    /** Ticks before the same target can be hit again by one charge. */
    private static final int CHARGE_HIT_COOLDOWN = 10;
    private static final int MAX_JAB_TARGETS = 8;
    private static final Map<Entity, Vec3> LAST_POSITIONS = new WeakHashMap<>();
    private static final Map<LivingEntity, Map<Integer, Long>> CHARGE_HITS = new WeakHashMap<>();
    /** Logs spear decisions; enable with -Dfuturebackport.debugSpears=true. */
    private static final boolean DEBUG = Boolean.getBoolean("futurebackport.debugSpears");

    private static void debug(String message, Object... args) {
        if (DEBUG) io.github.kestalf.futurebackport.FutureBackport.LOGGER.info("[spear] " + message, args);
    }

    // ------------------------------------------------------------------ targeting

    /** Entities hit by a spear thrust from {@code attacker}: along the look direction, between the minimum and maximum reach. */
    public static List<Entity> findTargets(LivingEntity attacker, double minReach, double maxReach) {
        Vec3 eye = attacker.getEyePosition();
        Vec3 look = attacker.getLookAngle();
        Vec3 end = eye.add(look.scale(maxReach));
        AABB search = attacker.getBoundingBox().expandTowards(look.scale(maxReach)).inflate(1.0);
        List<Map.Entry<Entity, Double>> hits = new ArrayList<>();
        for (Entity target : attacker.level().getEntities(attacker, search, e -> e.isPickable() && !e.isSpectator() && e.isAlive())) {
            if (target == attacker.getVehicle() || attacker.isPassengerOfSameVehicle(target)) continue;
            AABB box = target.getBoundingBox().inflate(SpearMaterial.HITBOX_MARGIN + target.getPickRadius());
            Optional<Vec3> hit = box.clip(eye, end);
            if (hit.isEmpty()) continue;
            double distance = eye.distanceTo(hit.get());
            if (distance >= minReach && distance <= maxReach) hits.add(Map.entry(target, distance));
        }
        hits.sort(Comparator.comparingDouble(Map.Entry::getValue));
        return hits.stream().map(Map.Entry::getKey).limit(MAX_JAB_TARGETS).toList();
    }

    public static double reachFactor(LivingEntity entity) {
        return entity instanceof Player ? 1.0 : SpearMaterial.MOB_REACH_FACTOR;
    }

    // ------------------------------------------------------------------ jab

    /** Handles a jab requested by a client: damages the listed targets with a normal attack. */
    public static void jab(ServerPlayer player, List<Integer> targetIds, boolean lunge) {
        ItemStack stack = player.getMainHandItem();
        debug("jab from {} targets={} lunge={} strength={}", player.getName().getString(), targetIds, lunge, player.getAttackStrengthScale(0.5F));
        if (!(stack.getItem() instanceof SpearItem spear) || player.isSpectator()) return;
        if (player.getAttackStrengthScale(0.5F) < 0.9F) return;
        ServerLevel level = player.serverLevel();
        level.playSound(null, player.getX(), player.getY(), player.getZ(), spear.attackSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
        double maxReach = SpearMaterial.MAX_REACH + 2.0;
        int hits = 0;
        for (int id : targetIds) {
            Entity target = level.getEntity(id);
            if (target == null || target == player || !target.isAlive() || hits >= MAX_JAB_TARGETS) continue;
            if (target.getBoundingBox().distanceToSqr(player.getEyePosition()) > maxReach * maxReach) continue;
            boolean sprinting = player.isSprinting();
            player.setSprinting(false);
            player.attackStrengthTicker = 1000;
            player.attack(target);
            player.setSprinting(sprinting);
            hits++;
        }
        player.resetAttackStrengthTicker();
        if (hits > 0) stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        if (lunge) applyLungeCosts(player, stack);
    }

    // ------------------------------------------------------------------ lunge

    public static int lungeLevel(Level level, ItemStack stack) {
        Optional<Holder.Reference<Enchantment>> lunge = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(ModKeys.LUNGE);
        return lunge.map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, stack)).orElse(0);
    }

    public static boolean canLunge(Player player) {
        return !player.isPassenger() && !player.isFallFlying() && !player.isInWater()
                && (player.getAbilities().instabuild || player.getFoodData().getFoodLevel() > 6);
    }

    /** Applies the Lunge impulse; called on the client (players control their own movement). */
    public static boolean applyLungeImpulse(Player player, ItemStack stack) {
        int level = lungeLevel(player.level(), stack);
        if (level <= 0 || !canLunge(player)) return false;
        Vec3 look = player.getLookAngle();
        double strength = 0.458 * level;
        player.setDeltaMovement(player.getDeltaMovement().add(look.x * strength, 0.0, look.z * strength));
        return true;
    }

    private static void applyLungeCosts(ServerPlayer player, ItemStack stack) {
        int level = lungeLevel(player.level(), stack);
        if (level <= 0 || !canLunge(player)) return;
        if (!player.getAbilities().instabuild) player.causeFoodExhaustion(4.0F * level);
        stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.event("item.spear.lunge_" + Math.min(level, 3)).get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ charge

    /** Records the position of a player at the end of a tick so its speed can be measured during charges. */
    public static void trackPosition(Entity entity) {
        LAST_POSITIONS.put(entity, entity.position());
    }

    /** Velocity in blocks per tick. The server does not simulate player movement, so it is measured from positions. */
    private static Vec3 velocity(Entity entity) {
        if (entity instanceof Player) {
            Vec3 last = LAST_POSITIONS.get(entity);
            return last == null ? Vec3.ZERO : entity.position().subtract(last);
        }
        return entity.getRootVehicle().getDeltaMovement();
    }

    public static void chargeTick(LivingEntity attacker, ItemStack stack, SpearItem spear, int useTicks) {
        SpearMaterial material = spear.material();
        if (useTicks >= material.damageDuration()) {
            attacker.releaseUsingItem();
            if (attacker instanceof Player player) player.getCooldowns().addCooldown(spear, material.chargeDelay() * 2);
            return;
        }
        if (useTicks < material.chargeDelay()) return;
        debug("charge tick {} by {} velocity={}", useTicks, attacker.getName().getString(), velocity(attacker));

        double reach = reachFactor(attacker);
        double speedFactor = attacker instanceof Player ? 1.0 : SpearMaterial.MOB_SPEED_FACTOR;
        Vec3 look = attacker.getLookAngle();
        Vec3 attackerVelocity = velocity(attacker);
        double attackerSpeed = attackerVelocity.length() * 20.0;
        ServerLevel level = (ServerLevel) attacker.level();
        Map<Integer, Long> recentHits = CHARGE_HITS.computeIfAbsent(attacker, a -> new HashMap<>());
        long now = level.getGameTime();

        for (Entity target : findTargets(attacker, SpearMaterial.MIN_REACH * reach, SpearMaterial.MAX_REACH * reach)) {
            Long lastHit = recentHits.get(target.getId());
            if (lastHit != null && now - lastHit < CHARGE_HIT_COOLDOWN) continue;
            double relativeSpeed = attackerVelocity.subtract(velocity(target)).dot(look) * 20.0;
            boolean damage = relativeSpeed >= SpearMaterial.MIN_RELATIVE_SPEED * speedFactor;
            boolean knockback = useTicks < material.knockbackDuration() && attackerSpeed >= SpearMaterial.KNOCKBACK_SPEED * speedFactor;
            boolean dismount = useTicks < material.dismountDuration() && attackerSpeed >= material.dismountSpeed() * speedFactor;
            debug("  target {} relativeSpeed={} damage={} knockback={}", target.getName().getString(), relativeSpeed, damage, knockback);
            if (!damage && !knockback) continue;
            recentHits.put(target.getId(), now);

            if (damage) {
                DamageSource source = level.damageSources().source(ModKeys.SPEAR, attacker);
                float amount = EnchantmentHelper.modifyDamage(level, stack, target, source,
                        (float) (material.damageMultiplier() * relativeSpeed));
                if (target.hurt(source, amount)) {
                    EnchantmentHelper.doPostAttackEffects(level, target, source);
                    stack.hurtAndBreak(1, attacker, attacker.getUsedItemHand() == InteractionHand.MAIN_HAND
                            ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                    if (attacker instanceof Player player) player.setLastHurtMob(target);
                }
            }
            if (knockback && target instanceof LivingEntity living) {
                living.knockback(0.5, -look.x, -look.z);
            }
            if (dismount && target.isPassenger()) {
                target.stopRiding();
            }
            level.playSound(null, target.getX(), target.getY(), target.getZ(), spear.hitSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
            if (stack.isEmpty()) break;
        }
    }

    public static void endCharge(LivingEntity attacker) {
        CHARGE_HITS.remove(attacker);
    }

    private SpearCombat() {
    }
}
