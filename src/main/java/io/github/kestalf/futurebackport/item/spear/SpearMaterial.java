package io.github.kestalf.futurebackport.item.spear;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/**
 * Stats of one spear tier, taken from the {@code kinetic_weapon}/{@code attack_range} components of 1.21.11+.
 * Durations are in ticks counted from the start of the charge.
 *
 * @param jabDamage          attack damage added to the player's base damage for a jab
 * @param attackSpeed        attack speed modifier (jab cooldown)
 * @param chargeDelay        ticks before a charge can hit anything
 * @param damageMultiplier   charge damage per block/second of relative speed
 * @param damageDuration     the charge deals damage until this tick
 * @param knockbackDuration  the charge knocks back until this tick
 * @param dismountDuration   the charge dismounts riders until this tick
 * @param dismountSpeed      minimal speed (blocks/second) of the attacker to dismount a rider
 */
public record SpearMaterial(String name, int durability, int enchantability, float jabDamage, float attackSpeed,
                            int chargeDelay, float damageMultiplier, int damageDuration, int knockbackDuration,
                            int dismountDuration, float dismountSpeed, Supplier<Ingredient> repair, boolean wooden) {
    public static final float MIN_REACH = 2.0F;
    public static final float MAX_REACH = 4.5F;
    public static final float HITBOX_MARGIN = 0.125F;
    public static final float MOB_REACH_FACTOR = 0.5F;
    public static final float MIN_RELATIVE_SPEED = 4.6F;
    public static final float KNOCKBACK_SPEED = 5.1F;
    public static final float MOB_SPEED_FACTOR = 0.2F;

    public static final SpearMaterial WOOD = new SpearMaterial("wooden", 59, 15, 0.0F, -2.4615384F, 15, 0.7F, 300, 200, 100, 14.0F,
            () -> Ingredient.of(ItemTags.PLANKS), true);
    public static final SpearMaterial STONE = new SpearMaterial("stone", 131, 5, 1.0F, -2.6666667F, 14, 0.82F, 275, 180, 90, 13.0F,
            () -> Ingredient.of(ItemTags.STONE_TOOL_MATERIALS), false);
    public static final SpearMaterial COPPER = new SpearMaterial("copper", 190, 13, 1.0F, -2.8235295F, 13, 0.82F, 250, 165, 80, 12.0F,
            () -> Ingredient.of(Items.COPPER_INGOT), false);
    public static final SpearMaterial IRON = new SpearMaterial("iron", 250, 14, 2.0F, -2.9473684F, 12, 0.95F, 225, 135, 50, 11.0F,
            () -> Ingredient.of(Items.IRON_INGOT), false);
    public static final SpearMaterial GOLD = new SpearMaterial("golden", 32, 22, 0.0F, -2.9473684F, 14, 0.7F, 275, 170, 70, 13.0F,
            () -> Ingredient.of(Items.GOLD_INGOT), false);
    public static final SpearMaterial DIAMOND = new SpearMaterial("diamond", 1561, 10, 3.0F, -3.047619F, 10, 1.075F, 200, 130, 60, 10.0F,
            () -> Ingredient.of(Items.DIAMOND), false);
    public static final SpearMaterial NETHERITE = new SpearMaterial("netherite", 2031, 15, 4.0F, -3.1304348F, 8, 1.2F, 175, 110, 50, 9.0F,
            () -> Ingredient.of(Items.NETHERITE_INGOT), false);
}
