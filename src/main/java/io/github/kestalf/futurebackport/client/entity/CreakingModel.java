package io.github.kestalf.futurebackport.client.entity;

import io.github.kestalf.futurebackport.entity.creaking.Creaking;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Creaking model; geometry from the official Bedrock model (identical parts and texture layout). */
public class CreakingModel extends HierarchicalModel<Creaking> {
    private final ModelPart root;
    private final ModelPart head;

    public CreakingModel(ModelPart root) {
        this.root = root.getChild("root");
        this.head = this.root.getChild("upper_body").getChild("head");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot().addOrReplaceChild("root", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
        PartDefinition upperBody = root.addOrReplaceChild("upper_body", CubeListBuilder.create(), PartPose.offset(-1.0F, -19.0F, 0.0F));
        upperBody.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -10.0F, -3.0F, 6.0F, 10.0F, 6.0F)
                .texOffs(28, 31).addBox(-3.0F, -13.0F, -3.0F, 6.0F, 3.0F, 6.0F)
                .texOffs(12, 40).addBox(3.0F, -13.0F, 0.0F, 9.0F, 14.0F, 0.0F)
                .texOffs(34, 12).addBox(-12.0F, -14.0F, 0.0F, 9.0F, 14.0F, 0.0F), PartPose.offset(-3.0F, -11.0F, 0.0F));
        upperBody.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 16).addBox(0.0F, -3.0F, -3.0F, 6.0F, 13.0F, 5.0F)
                .texOffs(24, 0).addBox(-6.0F, -4.0F, -3.0F, 6.0F, 7.0F, 5.0F), PartPose.offset(0.0F, -7.0F, 1.0F));
        upperBody.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(22, 13).addBox(-2.0F, -1.5F, -1.5F, 3.0F, 21.0F, 3.0F)
                .texOffs(46, 0).addBox(-2.0F, 19.5F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offset(-7.0F, -9.5F, 1.5F));
        upperBody.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(30, 40).addBox(0.0F, -1.0F, -1.5F, 3.0F, 16.0F, 3.0F)
                .texOffs(52, 12).addBox(0.0F, -5.0F, -1.5F, 3.0F, 4.0F, 3.0F)
                .texOffs(52, 19).addBox(0.0F, 15.0F, -1.5F, 3.0F, 4.0F, 3.0F), PartPose.offset(6.0F, -9.0F, 0.5F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(42, 40).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 16.0F, 3.0F)
                .texOffs(45, 55).addBox(-1.5F, 15.7F, -4.5F, 5.0F, 0.0F, 9.0F), PartPose.offset(1.5F, -16.0F, 0.5F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 34).addBox(-3.0F, -1.5F, -1.5F, 3.0F, 19.0F, 3.0F)
                .texOffs(45, 46).addBox(-5.0F, 17.2F, -4.5F, 5.0F, 0.0F, 9.0F)
                .texOffs(12, 34).addBox(-3.0F, -4.5F, -1.5F, 3.0F, 3.0F, 3.0F), PartPose.offset(-1.0F, -17.5F, 0.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(Creaking creaking, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        if (creaking.canMove()) {
            head.xRot = headPitch * Mth.DEG_TO_RAD;
            head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            animateWalk(CreakingAnimations.WALK, limbSwing, limbSwingAmount, 1.0F, 1.0F);
        }
        animate(creaking.attackAnimationState, CreakingAnimations.ATTACK, ageInTicks);
        animate(creaking.invulnerabilityAnimationState, CreakingAnimations.INVULNERABLE, ageInTicks);
        animate(creaking.deathAnimationState, CreakingAnimations.DEATH, ageInTicks);
    }
}
