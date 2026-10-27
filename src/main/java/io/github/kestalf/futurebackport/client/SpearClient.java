package io.github.kestalf.futurebackport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.item.spear.SpearCombat;
import io.github.kestalf.futurebackport.item.spear.SpearItem;
import io.github.kestalf.futurebackport.item.spear.SpearMaterial;
import io.github.kestalf.futurebackport.network.SpearJabPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Client side of spears: jab input, first-person animation and the third-person charge pose. */
@EventBusSubscriber(modid = FutureBackport.MOD_ID, value = Dist.CLIENT)
public final class SpearClient {
    /** Holding the attack button jabs only once per press. */
    private static boolean jabbedThisPress;

    @SubscribeEvent
    static void onAttackKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() || event.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isSpectator()) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof SpearItem)) return;
        // Spears never break blocks and handle hits themselves.
        event.setCanceled(true);
        event.setSwingHand(false);
        if (jabbedThisPress) return;
        jabbedThisPress = true;
        jab(player, stack);
    }

    /** Performs a jab if the attack cooldown is over. */
    public static void jab(LocalPlayer player, ItemStack stack) {
        if (player.getAttackStrengthScale(0.0F) < 1.0F) return;
        List<Integer> targets = SpearCombat.findTargets(player, SpearMaterial.MIN_REACH, SpearMaterial.MAX_REACH).stream()
                .map(Entity::getId).toList();
        boolean lunge = SpearCombat.applyLungeImpulse(player, stack);
        // The jab must reach the server before the swing packet, which resets the server-side attack cooldown.
        PacketDistributor.sendToServer(new SpearJabPayload(targets, lunge));
        player.swing(InteractionHand.MAIN_HAND);
        player.resetAttackStrengthTicker();
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().options.keyAttack.isDown()) jabbedThisPress = false;
    }

    /** Render extensions registered for every spear item. */
    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        @Override
        public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
            return entity.isUsingItem() && entity.getUseItem() == stack ? ModArmPoses.SPEAR_CHARGE.getValue() : null;
        }

        @Override
        public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm, ItemStack stack, float partialTick,
                                               float equipProcess, float swingProcess) {
            int side = arm == HumanoidArm.RIGHT ? 1 : -1;
            pose.translate(side * 0.56F, -0.52F + equipProcess * -0.6F, -0.72F);
            if (player.isUsingItem() && player.getUseItem() == stack && stack.getItem() instanceof SpearItem spear) {
                SpearMaterial material = spear.material();
                float ticks = player.getTicksUsingItem() + partialTick;
                float raise = Mth.clamp(ticks / material.chargeDelay(), 0.0F, 1.0F);
                // Lowered and pointed forward; shaking while "tired", dropping when "disengaged".
                pose.translate(-side * 0.2F * raise, -0.1F * raise, 0.0F);
                pose.mulPose(Axis.XP.rotationDegrees(-60.0F * raise));
                pose.mulPose(Axis.YP.rotationDegrees(side * -10.0F * raise));
                if (ticks >= material.dismountDuration() && ticks < material.knockbackDuration()) {
                    pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(ticks * 1.7F) * 2.0F));
                } else if (ticks >= material.knockbackDuration()) {
                    float drop = Mth.clamp((ticks - material.knockbackDuration()) / 10.0F, 0.0F, 1.0F);
                    pose.translate(0.0F, -0.25F * drop, 0.0F);
                    pose.mulPose(Axis.XP.rotationDegrees(20.0F * drop));
                }
            } else if (swingProcess > 0.0F) {
                // Jab: a straight thrust instead of the usual swing arc.
                float thrust = Mth.sin(swingProcess * (float) Math.PI);
                pose.translate(-side * 0.1F * thrust, 0.05F * thrust, -0.45F * thrust);
            }
            return true;
        }
    };

    private SpearClient() {
    }
}
