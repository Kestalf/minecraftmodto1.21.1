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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Happy ghast: a 16-unit cube with nine tentacles (like the ghast) and an inner body that shows through the
 * transparent parts of the ghastling texture. Texture coordinates are laid out on a 64x64 grid.
 */
public class HappyGhastModel extends HierarchicalModel<HappyGhast> {
    private final ModelPart root;
    private final ModelPart[] tentacles = new ModelPart[9];

    public HappyGhastModel(ModelPart root) {
        this.root = root;
        for (int i = 0; i < tentacles.length; i++) tentacles[i] = root.getChild("tentacle" + i);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F),
                PartPose.offset(0.0F, 17.6F, 0.0F));
        root.addOrReplaceChild("inner_body", CubeListBuilder.create().texOffs(0, 32)
                .addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F, new CubeDeformation(-0.5F)), PartPose.offset(0.0F, 17.6F, 0.0F));
        RandomSource random = RandomSource.create(1660L);
        for (int i = 0; i < 9; i++) {
            float x = (((i % 3) - (i / 3 % 2) * 0.5F + 0.25F) / 2.0F * 2.0F - 1.0F) * 5.0F;
            float z = ((i / 3) / 2.0F * 2.0F - 1.0F) * 5.0F;
            int length = random.nextInt(7) + 8;
            root.addOrReplaceChild("tentacle" + i, CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, length, 2.0F),
                    PartPose.offset(x, 24.6F, z));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(HappyGhast ghast, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        for (int i = 0; i < tentacles.length; i++) {
            tentacles[i].xRot = 0.2F * Mth.sin(ageInTicks * 0.3F + i) + 0.4F;
        }
    }

    @Override
    public ModelPart root() {
        return root;
    }
}
