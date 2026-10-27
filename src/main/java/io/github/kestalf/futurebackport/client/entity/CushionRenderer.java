package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.Cushion;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;

import java.util.EnumMap;
import java.util.Map;

/** A 16x4x16 pillow (official Bedrock geometry) with the texture of its color. */
public class CushionRenderer extends EntityRenderer<Cushion> {
    private static final Map<DyeColor, ResourceLocation> TEXTURES = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            TEXTURES.put(color, FutureBackport.id("textures/entity/cushion/" + color.getSerializedName() + "_cushion.png"));
        }
    }

    private final ModelPart model;

    public CushionRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = context.bakeLayer(ModModelLayers.CUSHION);
        this.shadowRadius = 0.0F;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cushion", CubeListBuilder.create().texOffs(0, 0)
                .addBox(-8.0F, -3.875F, -8.0F, 16.0F, 4.0F, 16.0F, new CubeDeformation(-0.01F)), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void render(Cushion cushion, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - cushion.getYRot()));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        model.render(poseStack, buffer.getBuffer(RenderType.entityCutout(getTextureLocation(cushion))), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(cushion, entityYaw, partialTick, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(Cushion cushion) {
        return TEXTURES.get(cushion.getColor());
    }
}
