package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.golem.CopperGolem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;

public class CopperGolemRenderer extends MobRenderer<CopperGolem, CopperGolemModel<CopperGolem>> {
    public CopperGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new CopperGolemModel<>(context.bakeLayer(ModModelLayers.COPPER_GOLEM)), 0.3F);
        addLayer(new EyesLayer(this));
        addLayer(new HeldItemLayer(this, context.getItemInHandRenderer()));
    }

    /** {@code textures/entity/copper_golem/copper_golem[_<stage>][suffix].png} */
    public static ResourceLocation texture(WeatherState state, String suffix) {
        String stage = state == WeatherState.UNAFFECTED ? "" : "_" + state.getSerializedName();
        return FutureBackport.id("textures/entity/copper_golem/copper_golem" + suffix + stage + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(CopperGolem golem) {
        return texture(golem.getWeatherState(), "");
    }

    private static final class EyesLayer extends RenderLayer<CopperGolem, CopperGolemModel<CopperGolem>> {
        EyesLayer(RenderLayerParent<CopperGolem, CopperGolemModel<CopperGolem>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, CopperGolem golem, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (golem.isInvisible()) return;
            getParentModel().renderToBuffer(poseStack, buffer.getBuffer(RenderType.eyes(texture(golem.getWeatherState(), "_eyes"))), 0xF00000,
                    LivingEntityRenderer.getOverlayCoords(golem, 0.0F));
        }
    }

    private static final class HeldItemLayer extends RenderLayer<CopperGolem, CopperGolemModel<CopperGolem>> {
        private final ItemInHandRenderer itemRenderer;

        HeldItemLayer(RenderLayerParent<CopperGolem, CopperGolemModel<CopperGolem>> parent, ItemInHandRenderer itemRenderer) {
            super(parent);
            this.itemRenderer = itemRenderer;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, CopperGolem golem, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            ItemStack stack = golem.getMainHandItem();
            if (stack.isEmpty()) return;
            poseStack.pushPose();
            getParentModel().translateToHand(poseStack);
            poseStack.translate(0.0F, 0.0625F, 0.0625F);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            poseStack.scale(0.75F, 0.75F, 0.75F);
            itemRenderer.renderItem(golem, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, false, poseStack, buffer, light);
            poseStack.popPose();
        }
    }
}
