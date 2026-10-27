package io.github.kestalf.futurebackport.item.spear;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Spear (1.21.11). Left click performs a jab with a long but not short reach (see {@link SpearCombat#jab}); holding
 * right click lowers the spear for a charge attack whose damage depends on the speed difference between attacker and
 * target (see {@link SpearCombat#chargeTick}).
 */
public class SpearItem extends Item {
    private final SpearMaterial material;

    public SpearItem(SpearMaterial material, Properties properties) {
        super(properties.durability(material.durability()).attributes(createAttributes(material)));
        this.material = material;
    }

    public static ItemAttributeModifiers createAttributes(SpearMaterial material) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, material.jabDamage(),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, material.attackSpeed(),
                        AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(FutureBackport.id("spear_reach"),
                        SpearMaterial.MAX_REACH - 3.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .build();
    }

    public SpearMaterial material() {
        return material;
    }

    public SoundEvent attackSound() {
        return ModSounds.event(material.wooden() ? "item.spear_wood.attack" : "item.spear.attack").get();
    }

    public SoundEvent hitSound() {
        return ModSounds.event(material.wooden() ? "item.spear_wood.hit" : "item.spear.hit").get();
    }

    public SoundEvent useSound() {
        return ModSounds.event(material.wooden() ? "item.spear_wood.use" : "item.spear.use").get();
    }

    @Override
    public int getEnchantmentValue() {
        return material.enchantability();
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return material.repair().get().test(repairCandidate) || super.isValidRepairItem(stack, repairCandidate);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return false;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        level.playSound(player, player.getX(), player.getY(), player.getZ(), useSound(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide) SpearCombat.chargeTick(entity, stack, this, getUseDuration(stack, entity) - remainingUseDuration);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        SpearCombat.endCharge(entity);
    }
}
