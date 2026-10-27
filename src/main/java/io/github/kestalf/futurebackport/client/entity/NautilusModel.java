package io.github.kestalf.futurebackport.client.entity;

import io.github.kestalf.futurebackport.entity.nautilus.AbstractNautilus;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
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
import org.joml.Vector3f;

/**
 * Nautilus models from the official Bedrock geometry: the shell ({@code head}) in front and the tentacle body with
 * its mouth behind. The saddle, armor and coral layers share the part names, so the same animations apply to them.
 */
public class NautilusModel extends HierarchicalModel<AbstractNautilus> {
    private static final CubeDeformation MOUTH = new CubeDeformation(-0.002F);
    private static final Vector3f VECTOR_CACHE = new Vector3f();
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;

    public NautilusModel(ModelPart root) {
        this.root = root.hasChild("nautilus") ? root.getChild("nautilus") : root.getChild("nautilus_baby");
        this.head = this.root.getChild("head");
        this.body = this.root.hasChild("body") ? this.root.getChild("body") : null;
    }

    private static PartDefinition adultShell(PartDefinition nautilus, CubeDeformation deformation, boolean full) {
        CubeListBuilder shell = CubeListBuilder.create().texOffs(0, 0).addBox(-7.0F, -10.0F, -7.0F, 14.0F, 10.0F, 16.0F, deformation);
        if (full) {
            shell.texOffs(0, 26).addBox(-7.0F, 0.0F, -7.0F, 14.0F, 8.0F, 20.0F).texOffs(48, 26).addBox(-7.0F, 0.0F, 6.0F, 14.0F, 8.0F, 0.0F);
        }
        return nautilus.addOrReplaceChild("head", shell, PartPose.offset(0.0F, -13.0F, 5.0F));
    }

    private static void adultBody(PartDefinition nautilus) {
        PartDefinition body = nautilus.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 54).addBox(-5.0F, -4.51F, -3.0F, 10.0F, 8.0F, 14.0F)
                .texOffs(0, 76).addBox(-5.0F, -4.51F, 7.0F, 10.0F, 8.0F, 0.0F), PartPose.offset(0.0F, -8.5F, 12.3F));
        body.addOrReplaceChild("mouth_top", CubeListBuilder.create().texOffs(54, 54).addBox(-5.0F, -2.0F, 0.0F, 10.0F, 4.0F, 4.0F, MOUTH),
                PartPose.offset(0.0F, -2.51F, 7.0F));
        body.addOrReplaceChild("inner_mouth", CubeListBuilder.create().texOffs(54, 70).addBox(-3.0F, -2.0F, -0.5F, 6.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, -0.51F, 7.5F));
        body.addOrReplaceChild("mouth_bottom", CubeListBuilder.create().texOffs(54, 62).addBox(-5.0F, -1.98F, 0.0F, 10.0F, 4.0F, 4.0F, MOUTH),
                PartPose.offset(0.0F, 1.49F, 7.0F));
    }

    private static PartDefinition adultRoot(MeshDefinition mesh) {
        return mesh.getRoot().addOrReplaceChild("nautilus", CubeListBuilder.create(), PartPose.offset(0.0F, 29.0F, -6.0F));
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition nautilus = adultRoot(mesh);
        adultShell(nautilus, CubeDeformation.NONE, true);
        adultBody(nautilus);
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createSaddleLayer() {
        MeshDefinition mesh = new MeshDefinition();
        adultShell(adultRoot(mesh), new CubeDeformation(0.2F), false);
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createArmorLayer() {
        MeshDefinition mesh = new MeshDefinition();
        adultShell(adultRoot(mesh), CubeDeformation.NONE, true);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** The zombie nautilus from warm oceans, overgrown with yellow, pink, blue and red coral. */
    public static LayerDefinition createCoralLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition nautilus = adultRoot(mesh);
        PartDefinition head = adultShell(nautilus, CubeDeformation.NONE, true);
        PartDefinition corals = head.addOrReplaceChild("corals", CubeListBuilder.create(), PartPose.offset(8.0F, 4.5F, -8.0F));
        PartDefinition yellow = corals.addOrReplaceChild("yellow_coral", CubeListBuilder.create(), PartPose.offset(0.0F, -11.0F, 11.0F));
        yellow.addOrReplaceChild("yellow_coral_r0", CubeListBuilder.create().texOffs(0, 85).addBox(-4.5F, -3.5F, 0.0F, 6.0F, 8.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 45.0F * Mth.DEG_TO_RAD, 0.0F));
        yellow.addOrReplaceChild("yellow_coral_r1", CubeListBuilder.create().texOffs(0, 85).addBox(-4.5F, -3.5F, 0.0F, 6.0F, 8.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 2.0F, 0.0F, -45.0F * Mth.DEG_TO_RAD, 0.0F));
        PartDefinition pink = corals.addOrReplaceChild("pink_coral", CubeListBuilder.create().texOffs(-8, 94)
                .addBox(-4.5F, 4.5F, 0.0F, 6.0F, 0.0F, 8.0F), PartPose.offset(-12.5F, -18.0F, 11.0F));
        pink.addOrReplaceChild("pink_coral_r0", CubeListBuilder.create().texOffs(-8, 94).addBox(-3.0F, 0.0F, -4.0F, 6.0F, 0.0F, 8.0F),
                PartPose.offsetAndRotation(-1.5F, 4.5F, 4.0F, 0.0F, 0.0F, 90.0F * Mth.DEG_TO_RAD));
        PartDefinition blue = corals.addOrReplaceChild("blue_coral", CubeListBuilder.create(), PartPose.offset(-14.0F, 0.0F, 5.5F));
        blue.addOrReplaceChild("blue_coral_r0", CubeListBuilder.create().texOffs(0, 102).addBox(-3.5F, -5.5F, 0.0F, 5.0F, 10.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, -45.0F * Mth.DEG_TO_RAD, 0.0F));
        blue.addOrReplaceChild("blue_coral_r1", CubeListBuilder.create().texOffs(0, 102).addBox(-3.5F, -5.5F, 0.0F, 5.0F, 10.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, -2.0F, 0.0F, 45.0F * Mth.DEG_TO_RAD, 0.0F));
        PartDefinition red = corals.addOrReplaceChild("red_coral", CubeListBuilder.create(), PartPose.ZERO);
        red.addOrReplaceChild("red_coral_r0", CubeListBuilder.create().texOffs(0, 112).addBox(-4.5F, -5.5F, 0.0F, 6.0F, 10.0F, 0.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 45.0F * Mth.DEG_TO_RAD, 0.0F));
        red.addOrReplaceChild("red_coral_r1", CubeListBuilder.create().texOffs(0, 112).addBox(-2.5F, -5.5F, 0.0F, 4.0F, 10.0F, 0.0F),
                PartPose.offsetAndRotation(-0.5F, -1.0F, 1.5F, 0.0F, -47.5F * Mth.DEG_TO_RAD, 0.0F));
        adultBody(nautilus);
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createBabyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition nautilus = mesh.getRoot().addOrReplaceChild("nautilus_baby", CubeListBuilder.create(), PartPose.offset(0.0F, 28.0F, 0.0F));
        nautilus.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-1.0F, -4.0F, -1.0F, 7.0F, 4.0F, 7.0F)
                .texOffs(0, 11).addBox(-1.0F, 0.0F, -1.0F, 7.0F, 4.0F, 9.0F)
                .texOffs(23, 11).addBox(-1.0F, 0.0F, 5.0F, 7.0F, 4.0F, 0.0F), PartPose.offset(-2.5F, -8.0F, -2.5F));
        PartDefinition body = nautilus.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 24).addBox(-2.5F, -3.01F, -1.0F, 5.0F, 4.0F, 7.0F)
                .texOffs(0, 35).addBox(-2.5F, -3.01F, 4.1F, 5.0F, 4.0F, 0.0F), PartPose.offset(0.0F, -5.0F, 2.5F));
        body.addOrReplaceChild("mouth_top", CubeListBuilder.create().texOffs(24, 24).addBox(-2.5F, -1.0F, 0.0F, 5.0F, 2.0F, 2.0F, MOUTH),
                PartPose.offset(0.0F, -2.01F, 3.9F));
        body.addOrReplaceChild("inner_mouth", CubeListBuilder.create().texOffs(24, 32).addBox(-1.5F, -1.0F, -1.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, -1.01F, 4.9F));
        body.addOrReplaceChild("mouth_bottom", CubeListBuilder.create().texOffs(24, 28).addBox(-2.5F, -1.0F, 0.0F, 5.0F, 2.0F, 2.0F, MOUTH),
                PartPose.offset(0.0F, -0.01F, 3.9F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(AbstractNautilus nautilus, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        if (body != null) body.xRot = headPitch * Mth.DEG_TO_RAD;
        KeyframeAnimations.animate(this, BREATHE, (long) (ageInTicks * 50.0F), 1.0F, VECTOR_CACHE);
        animate(nautilus.swimAnimationState, SWIM, ageInTicks);
    }

    // Converted from the Bedrock animation file.
    static final AnimationDefinition BREATHE = AnimationDefinition.Builder.withLength(1.0F)
            .looping()
            .addAnimation("body", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.2F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 0.9F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.875F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("mouth_top", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("mouth_top", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.4F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 0.9F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.875F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("inner_mouth", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.scaleVec(0.8F, 0.8F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 0.9F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.875F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("mouth_bottom", new AnimationChannel(AnimationChannel.Targets.ROTATION,
                    new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.degreeVec(-30.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .addAnimation("mouth_bottom", new AnimationChannel(AnimationChannel.Targets.SCALE,
                    new Keyframe(0.0F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.4F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.75F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 0.9F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.875F, KeyframeAnimations.scaleVec(1.0F, 1.0F, 1.0F), AnimationChannel.Interpolations.LINEAR)))
            .build();

    static final AnimationDefinition SWIM = AnimationDefinition.Builder.withLength(1.0F)
            .looping()
            .addAnimation("head", new AnimationChannel(AnimationChannel.Targets.POSITION,
                    new Keyframe(0.0F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.5F, KeyframeAnimations.posVec(0.0F, 0.0F, 2.0F), AnimationChannel.Interpolations.LINEAR),
                    new Keyframe(0.8125F, KeyframeAnimations.posVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)))
            .build();
}
