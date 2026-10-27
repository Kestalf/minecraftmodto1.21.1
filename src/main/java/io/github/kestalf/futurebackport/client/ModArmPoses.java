package io.github.kestalf.futurebackport.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;

/** Arm poses added through NeoForge's enum extension (see META-INF/enumextensions.json). */
public final class ModArmPoses {
    /** Spear lowered for a charge: the arm points forward, following the head. */
    public static final EnumProxy<HumanoidModel.ArmPose> SPEAR_CHARGE = new EnumProxy<>(HumanoidModel.ArmPose.class, false,
            (IArmPoseTransformer) ModArmPoses::spearCharge);

    private static void spearCharge(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        ModelPart part = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
        part.xRot = -(float) Math.PI / 2.2F + model.head.xRot * 0.6F;
        part.yRot = (arm == HumanoidArm.RIGHT ? -0.15F : 0.15F) + model.head.yRot;
    }

    private ModArmPoses() {
    }
}
