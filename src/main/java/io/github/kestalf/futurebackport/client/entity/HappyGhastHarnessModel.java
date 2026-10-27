package io.github.kestalf.futurebackport.client.entity;

import io.github.kestalf.futurebackport.entity.ghast.HappyGhast;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** The harness worn by a happy ghast: a slightly larger shell plus aviator goggles that drop over the eyes when ridden. */
public class HappyGhastHarnessModel extends HierarchicalModel<HappyGhast> {
    private static final float GOGGLES_DOWN_Y = 15.1F;
    private static final float GOGGLES_UP_Y = 10.6F;
    private final ModelPart root;
    private final ModelPart goggles;

    public HappyGhastHarnessModel(ModelPart root) {
        this.root = root;
        this.goggles = root.getChild("goggles");
    }

    public static LayerDefinition createHarnessLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("harness", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.25F)), PartPose.offset(0.0F, 17.6F, 0.0F));
        root.addOrReplaceChild("goggles", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-8.0F, -2.5F, -1.0F, 16.0F, 5.0F, 5.0F, new CubeDeformation(0.15F)), PartPose.offset(0.0F, GOGGLES_UP_Y, -8.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createRopesLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("ropes", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(0.4F)), PartPose.offset(0.0F, 17.6F, 0.0F));
        mesh.getRoot().addOrReplaceChild("goggles", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(HappyGhast ghast, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        goggles.y = ghast.areGogglesDown() ? GOGGLES_DOWN_Y : GOGGLES_UP_Y;
    }

    @Override
    public ModelPart root() {
        return root;
    }
}
