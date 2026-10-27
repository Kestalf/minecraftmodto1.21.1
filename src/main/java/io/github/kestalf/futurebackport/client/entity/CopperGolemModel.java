package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kestalf.futurebackport.entity.golem.CopperGolem;
import net.minecraft.client.animation.KeyframeAnimations;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/**
 * Copper golem model; geometry from the official Bedrock models. The statue poses (sitting, running, star) are
 * separate layers with the same part names; the standing statue uses the normal layer.
 */
public class CopperGolemModel<T extends Entity> extends HierarchicalModel<T> {
    private static final CubeDeformation ROD = new CubeDeformation(-0.01F);
    private static final org.joml.Vector3f VECTOR_CACHE = new org.joml.Vector3f();
    private final ModelPart root;
    private final ModelPart head;

    public CopperGolemModel(ModelPart root) {
        this.root = root.getChild("root");
        this.head = this.root.getChild("body").getChild("head");
    }

    private static CubeListBuilder head(float z) {
        return CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.0F, -5.0F + z, 8.0F, 5.0F, 10.0F)
                .texOffs(56, 0).addBox(-1.0F, -2.0F, -6.0F + z, 2.0F, 3.0F, 2.0F)
                .texOffs(37, 8).addBox(-1.0F, -9.0F, -1.0F + z, 2.0F, 4.0F, 2.0F, ROD)
                .texOffs(37, 0).addBox(-2.0F, -13.0F, -2.0F + z, 4.0F, 4.0F, 4.0F, ROD);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(1.0F, 24.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F),
                PartPose.offset(-1.0F, -5.0F, 0.0F));
        body.addOrReplaceChild("head", head(0.0F), PartPose.offset(0.0F, -6.0F, 0.0F));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(36, 16)
                .addBox(-3.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F), PartPose.offset(-4.0F, -6.0F, 0.0F));
        rightArm.addOrReplaceChild("right_item", CubeListBuilder.create(), PartPose.offset(-1.0F, 7.4F, -1.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(50, 16).addBox(0.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F),
                PartPose.offset(4.0F, -6.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 27).addBox(-1.9F, 0.0F, -1.99F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(-3.0F, -5.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 27).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offset(1.0F, -5.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createSittingLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(3, 19).addBox(-3.0F, -4.0F, -4.525F, 6.0F, 1.0F, 6.0F)
                .texOffs(0, 15).addBox(-4.0F, -3.0F, -3.525F, 8.0F, 6.0F, 6.0F), PartPose.offset(0.0F, -3.0F, 2.325F));
        body.addOrReplaceChild("body_r0", CubeListBuilder.create().texOffs(3, 18).addBox(-4.0F, -3.0F, -2.2F, 8.0F, 6.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, -4.325F, 0.0F, 0.0F, -180.0F * Mth.DEG_TO_RAD));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(37, 8).addBox(-1.0F, -7.0F, -3.3F, 2.0F, 4.0F, 2.0F)
                .texOffs(37, 0).addBox(-2.0F, -11.0F, -4.3F, 4.0F, 4.0F, 4.0F)
                .texOffs(0, 0).addBox(-4.0F, -3.0F, -7.325F, 8.0F, 5.0F, 10.0F)
                .texOffs(56, 0).addBox(-1.0F, 0.0F, -8.325F, 2.0F, 3.0F, 2.0F), PartPose.offset(0.0F, -6.0F, -0.2F));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-4.0F, -5.6F, -1.8F, 25.0F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("right_arm_r0", CubeListBuilder.create().texOffs(36, 16).addBox(-3.075F, -0.9733F, -1.9966F, 3.0F, 10.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0893F, 0.1198F, -60.0F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create(),
                PartPose.offsetAndRotation(4.0F, -5.6F, -1.7F, 25.0F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("left_arm_r0", CubeListBuilder.create().texOffs(50, 16).addBox(0.075F, -1.0443F, -1.8997F, 3.0F, 10.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -0.0015F, -0.0808F, -60.0F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-2.1F, -2.1F, -2.075F));
        rightLeg.addOrReplaceChild("right_leg_r0", CubeListBuilder.create().texOffs(0, 27).addBox(-2.0F, 0.975F, 0.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.05F, -1.9F, 1.075F, -90.0F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(2.0F, -2.0F, -2.075F));
        leftLeg.addOrReplaceChild("left_leg_r0", CubeListBuilder.create().texOffs(16, 27).addBox(-2.0F, 0.975F, 0.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.05F, -2.0F, 1.075F, -90.0F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createRunningLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(1.0F, 24.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(-1.0F, -5.0F, 0.0F));
        body.addOrReplaceChild("body_r0", CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -6.0F, -3.5F, 8.0F, 6.0F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 0.2F, 0.5F, 6.9222F * Mth.DEG_TO_RAD, 4.5966F * Mth.DEG_TO_RAD, -3.8639F * Mth.DEG_TO_RAD));
        body.addOrReplaceChild("head", head(-0.001F), PartPose.offset(-0.3F, -5.6F, -1.999F));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-4.0F, -6.0F, 0.0F));
        rightArm.addOrReplaceChild("right_arm_r0", CubeListBuilder.create().texOffs(36, 16).addBox(-3.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F),
                PartPose.offsetAndRotation(-0.4F, 0.0F, -1.0F, 57.5F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(4.0F, -6.0F, 0.0F));
        leftArm.addOrReplaceChild("left_arm_r0", CubeListBuilder.create().texOffs(50, 16).addBox(0.0F, -1.0F, -2.0F, 3.0F, 10.0F, 4.0F),
                PartPose.offsetAndRotation(-0.4F, 0.0F, 0.0F, -50.0F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-3.0F, -5.0F, 0.0F));
        rightLeg.addOrReplaceChild("right_leg_r0", CubeListBuilder.create().texOffs(0, 27).addBox(-1.9F, 0.0F, -1.09F, 4.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -0.9F, -50.0F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.0F, -5.0F, 0.0F));
        leftLeg.addOrReplaceChild("left_leg_r0", CubeListBuilder.create().texOffs(16, 27).addBox(-2.1F, 0.0F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 45.0F * Mth.DEG_TO_RAD, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createStarLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(1.0F, 24.0F, 0.0F));
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 15).addBox(-4.0F, -6.0F, -3.0F, 8.0F, 6.0F, 6.0F),
                PartPose.offset(-1.0F, -5.0F, 0.0F));
        body.addOrReplaceChild("head", head(0.0F), PartPose.offset(0.0F, -6.0F, 0.0F));
        PartDefinition rightArm = body.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-4.0F, -6.0F, 0.0F));
        rightArm.addOrReplaceChild("right_arm_r0", CubeListBuilder.create().texOffs(36, 16).addBox(-1.5F, -5.0F, -2.0F, 3.0F, 10.0F, 4.0F),
                PartPose.offsetAndRotation(1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 110.0F * Mth.DEG_TO_RAD));
        PartDefinition leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(4.0F, -6.0F, 0.0F));
        leftArm.addOrReplaceChild("left_arm_r0", CubeListBuilder.create().texOffs(50, 16).addBox(-1.5F, -5.0F, -2.0F, 3.0F, 10.0F, 4.0F),
                PartPose.offsetAndRotation(-1.0F, 1.0F, 0.0F, 0.0F, 0.0F, -110.0F * Mth.DEG_TO_RAD));
        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-3.0F, -5.0F, 0.0F));
        rightLeg.addOrReplaceChild("right_leg_r0", CubeListBuilder.create().texOffs(0, 27).addBox(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(-0.65F, 2.0F, 0.01F, 0.0F, 0.0F, 15.0F * Mth.DEG_TO_RAD));
        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.0F, -5.0F, 0.0F));
        leftLeg.addOrReplaceChild("left_leg_r0", CubeListBuilder.create().texOffs(16, 27).addBox(-2.0F, -2.5F, -2.0F, 4.0F, 5.0F, 4.0F),
                PartPose.offsetAndRotation(0.65F, 2.0F, 0.0F, 0.0F, 0.0F, -15.0F * Mth.DEG_TO_RAD));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if (!(entity instanceof CopperGolem golem)) return;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        boolean holding = !golem.getMainHandItem().isEmpty();
        animateWalk(holding ? CopperGolemAnimations.WALK_HOLDING_ITEM : CopperGolemAnimations.WALK, limbSwing, limbSwingAmount, 2.0F, 2.5F);
        if (holding && golem.getInteraction() == CopperGolem.Interaction.NONE) {
            KeyframeAnimations.animate(this, CopperGolemAnimations.HOLD_ITEM, (long) (ageInTicks * 50.0F), 1.0F, VECTOR_CACHE);
        }
        animate(golem.spinAnimationState, switch (golem.getWeatherState()) {
            case UNAFFECTED, EXPOSED -> CopperGolemAnimations.SPIN;
            case WEATHERED -> CopperGolemAnimations.SPIN_WEATHERED;
            case OXIDIZED -> CopperGolemAnimations.SPIN_OXIDIZED;
        }, ageInTicks);
        animate(golem.interactionAnimationState, switch (golem.getInteraction()) {
            case TAKING -> CopperGolemAnimations.TAKE_ITEM;
            case TAKING_FAILED -> CopperGolemAnimations.TAKE_ITEM_FAIL;
            case PUTTING -> CopperGolemAnimations.PUT_ITEM;
            case PUTTING_FAILED, NONE -> CopperGolemAnimations.PUT_ITEM_FAIL;
        }, ageInTicks);
    }

    /** Moves the pose stack to the golem's hand, where held items are drawn. */
    public void translateToHand(PoseStack poseStack) {
        root.translateAndRotate(poseStack);
        ModelPart body = root.getChild("body");
        body.translateAndRotate(poseStack);
        ModelPart arm = body.getChild("right_arm");
        arm.translateAndRotate(poseStack);
        arm.getChild("right_item").translateAndRotate(poseStack);
    }
}
