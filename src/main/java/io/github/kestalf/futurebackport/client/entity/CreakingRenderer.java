package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.creaking.Creaking;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

public class CreakingRenderer extends MobRenderer<Creaking, CreakingModel> {
    private static final ResourceLocation TEXTURE = FutureBackport.id("textures/entity/creaking/creaking.png");
    private static final ResourceLocation EYES = FutureBackport.id("textures/entity/creaking/creaking_eyes.png");

    public CreakingRenderer(EntityRendererProvider.Context context) {
        super(context, new CreakingModel(context.bakeLayer(ModModelLayers.CREAKING)), 0.6F);
        addLayer(new EyesLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Creaking creaking) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(Creaking creaking) {
        // Crumbling creakings twitch in place instead of falling over.
        return creaking.isTearingDown() ? 0.0F : super.getFlipDegrees(creaking);
    }

    /** Glowing eyes while the creaking is active (flickering while it crumbles). */
    private static final class EyesLayer extends RenderLayer<Creaking, CreakingModel> {
        EyesLayer(RenderLayerParent<Creaking, CreakingModel> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, Creaking creaking, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!creaking.hasGlowingEyes() || creaking.isInvisible()) return;
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(EYES)), 0xF00000,
                    LivingEntityRenderer.getOverlayCoords(creaking, 0.0F));
        }
    }
}
