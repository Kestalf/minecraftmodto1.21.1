package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.ghast.HappyGhast;
import io.github.kestalf.futurebackport.item.HarnessItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class HappyGhastRenderer extends MobRenderer<HappyGhast, HappyGhastModel> {
    private static final ResourceLocation TEXTURE = FutureBackport.id("textures/entity/ghast/happy_ghast.png");
    private static final ResourceLocation BABY_TEXTURE = FutureBackport.id("textures/entity/ghast/happy_ghast_baby.png");
    private static final ResourceLocation ROPES_TEXTURE = FutureBackport.id("textures/entity/ghast/happy_ghast_ropes.png");

    public HappyGhastRenderer(EntityRendererProvider.Context context) {
        super(context, new HappyGhastModel(context.bakeLayer(ModModelLayers.HAPPY_GHAST)), 1.5F);
        addLayer(new HarnessLayer(this, new HappyGhastHarnessModel(context.bakeLayer(ModModelLayers.HAPPY_GHAST_HARNESS)),
                new HappyGhastHarnessModel(context.bakeLayer(ModModelLayers.HAPPY_GHAST_ROPES))));
    }

    @Override
    public ResourceLocation getTextureLocation(HappyGhast ghast) {
        return ghast.isBaby() ? BABY_TEXTURE : TEXTURE;
    }

    @Override
    protected void scale(HappyGhast ghast, PoseStack poseStack, float partialTick) {
        float scale = 4.5F * ghast.getAgeScale();
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected RenderType getRenderType(HappyGhast ghast, boolean bodyVisible, boolean translucent, boolean glowing) {
        return bodyVisible ? RenderType.entityCutoutNoCull(getTextureLocation(ghast)) : super.getRenderType(ghast, bodyVisible, translucent, glowing);
    }

    private static final class HarnessLayer extends RenderLayer<HappyGhast, HappyGhastModel> {
        private final HappyGhastHarnessModel harness;
        private final HappyGhastHarnessModel ropes;

        HarnessLayer(RenderLayerParent<HappyGhast, HappyGhastModel> parent, HappyGhastHarnessModel harness, HappyGhastHarnessModel ropes) {
            super(parent);
            this.harness = harness;
            this.ropes = ropes;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, HappyGhast ghast, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(ghast.getBodyArmorItem().getItem() instanceof HarnessItem item)) return;
            ResourceLocation texture = FutureBackport.id("textures/entity/equipment/happy_ghast_body/" + item.color().getName() + "_harness.png");
            harness.setupAnim(ghast, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            harness.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(texture)), light,
                    LivingEntityRenderer.getOverlayCoords(ghast, 0.0F));
            if (ghast.isCarrying()) {
                ropes.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(ROPES_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            }
        }
    }
}
