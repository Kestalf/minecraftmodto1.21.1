package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.desert.Parched;
import net.minecraft.client.model.CamelModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.CamelRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.camel.Camel;

/** Renderers of the camel husk and the parched (both reuse the vanilla camel and skeleton rendering). */
public final class DesertMobRenderers {
    private static final ResourceLocation CAMEL_HUSK = FutureBackport.id("textures/entity/camel/camel_husk.png");
    private static final ResourceLocation CAMEL_HUSK_SADDLE = FutureBackport.id("textures/entity/equipment/camel_husk_saddle/saddle.png");
    private static final ResourceLocation PARCHED = FutureBackport.id("textures/entity/skeleton/parched.png");

    /**
     * The camel model with the camel husk texture. The 1.21.11+ textures keep the saddle in a separate texture that
     * uses the old camel saddle UVs, so the saddle is drawn as a second pass of the same model.
     */
    public static class CamelHuskRenderer extends CamelRenderer {
        public CamelHuskRenderer(EntityRendererProvider.Context context) {
            super(context, ModelLayers.CAMEL);
            addLayer(new SaddleLayer(this));
        }

        @Override
        public ResourceLocation getTextureLocation(Camel camel) {
            return CAMEL_HUSK;
        }
    }

    private static final class SaddleLayer extends RenderLayer<Camel, CamelModel<Camel>> {
        SaddleLayer(RenderLayerParent<Camel, CamelModel<Camel>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, Camel camel, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!camel.isSaddled() || camel.isInvisible()) return;
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(CAMEL_HUSK_SADDLE)), light,
                    LivingEntityRenderer.getOverlayCoords(camel, 0.0F));
        }
    }

    public static class ParchedRenderer extends SkeletonRenderer<Parched> {
        public ParchedRenderer(EntityRendererProvider.Context context) {
            super(context, ModModelLayers.PARCHED, ModelLayers.SKELETON_INNER_ARMOR, ModelLayers.SKELETON_OUTER_ARMOR);
        }

        @Override
        public ResourceLocation getTextureLocation(Parched parched) {
            return PARCHED;
        }
    }

    /** The parched in the skeleton's part layout; geometry from the official Bedrock model (hood, wraps, belt). */
    public static LayerDefinition createParchedLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 32).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F,
                new CubeDeformation(0.2F)), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F)
                .texOffs(28, 0).addBox(-4.0F, 10.0F, -2.0F, 8.0F, 1.0F, 4.0F)
                .texOffs(16, 48).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.025F)), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                .texOffs(42, 33).addBox(-1.55F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(-5.5F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(56, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                .texOffs(40, 48).addBox(-1.45F, -2.025F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(5.5F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                .texOffs(0, 49).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F)
                .texOffs(4, 49).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 12.0F, 3.0F), PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private DesertMobRenderers() {
    }
}
