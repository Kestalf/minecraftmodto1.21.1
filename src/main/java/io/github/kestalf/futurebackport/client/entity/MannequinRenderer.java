package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kestalf.futurebackport.entity.Mannequin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.component.ResolvableProfile;

/** Draws a mannequin like a player: its skin's model (wide or slim), armour, held items, pose and a line under its name. */
public class MannequinRenderer extends LivingEntityRenderer<Mannequin, PlayerModel<Mannequin>> {
    private static final PlayerSkin ALEX = new PlayerSkin(ResourceLocation.withDefaultNamespace("textures/entity/player/slim/alex.png"),
            null, null, null, PlayerSkin.Model.SLIM, true);
    private final PlayerModel<Mannequin> wide;
    private final PlayerModel<Mannequin> slim;

    public MannequinRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wide = model;
        this.slim = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        addLayer(new CustomHeadLayer<>(this, context.getModelSet(), context.getItemInHandRenderer()));
        addLayer(new ElytraLayer<>(this, context.getModelSet()));
    }

    private static PlayerSkin skin(Mannequin mannequin) {
        ResolvableProfile profile = mannequin.getProfile();
        return profile == null ? ALEX : Minecraft.getInstance().getSkinManager().getInsecureSkin(profile.gameProfile());
    }

    @Override
    public void render(Mannequin mannequin, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model = skin(mannequin).model() == PlayerSkin.Model.SLIM ? slim : wide;
        model.setAllVisible(true);
        model.hat.visible = mannequin.isLayerVisible("hat");
        model.jacket.visible = mannequin.isLayerVisible("jacket");
        model.leftSleeve.visible = mannequin.isLayerVisible("left_sleeve");
        model.rightSleeve.visible = mannequin.isLayerVisible("right_sleeve");
        model.leftPants.visible = mannequin.isLayerVisible("left_pants_leg");
        model.rightPants.visible = mannequin.isLayerVisible("right_pants_leg");
        model.crouching = mannequin.isCrouching();
        HumanoidModel.ArmPose main = mannequin.getMainHandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        HumanoidModel.ArmPose off = mannequin.getOffhandItem().isEmpty() ? HumanoidModel.ArmPose.EMPTY : HumanoidModel.ArmPose.ITEM;
        boolean right = mannequin.getMainArm() == HumanoidArm.RIGHT;
        model.rightArmPose = right ? main : off;
        model.leftArmPose = right ? off : main;
        super.render(mannequin, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Mannequin mannequin) {
        return skin(mannequin).texture();
    }

    @Override
    protected void scale(Mannequin mannequin, PoseStack pose, float partialTick) {
        pose.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected void setupRotations(Mannequin mannequin, PoseStack pose, float bob, float yBodyRot, float partialTick, float scale) {
        super.setupRotations(mannequin, pose, bob, yBodyRot, partialTick, scale);
        float xRot = mannequin.getViewXRot(partialTick);
        if (mannequin.hasPose(Pose.FALL_FLYING)) {
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F - xRot));
            return;
        }
        float swim = mannequin.getSwimAmount(partialTick);
        if (swim > 0.0F) {
            float target = mannequin.isInWater() ? -90.0F - xRot : -90.0F;
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(swim, 0.0F, target)));
            if (mannequin.isVisuallySwimming()) pose.translate(0.0F, -1.0F, 0.3F);
        }
    }

    @Override
    protected boolean shouldShowName(Mannequin mannequin) {
        return mannequin.isCustomNameVisible() && super.shouldShowName(mannequin);
    }

    @Override
    protected void renderNameTag(Mannequin mannequin, Component name, PoseStack pose, MultiBufferSource buffers, int light, float partialTick) {
        Component description = mannequin.getDescriptionLine();
        pose.pushPose();
        if (description != null && entityRenderDispatcher.distanceToSqr(mannequin) < 100.0) {
            super.renderNameTag(mannequin, description, pose, buffers, light, partialTick);
            pose.translate(0.0F, 9.0F * 1.15F * 0.025F, 0.0F);
        }
        super.renderNameTag(mannequin, name, pose, buffers, light, partialTick);
        pose.popPose();
    }
}
