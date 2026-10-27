package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.nautilus.AbstractNautilus;
import io.github.kestalf.futurebackport.entity.nautilus.ZombieNautilus;
import io.github.kestalf.futurebackport.item.NautilusArmorItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Nautilus and zombie nautilus, with the baby model, the coral variant and the saddle and armor layers. */
public class NautilusRenderer extends MobRenderer<AbstractNautilus, NautilusModel> {
    private static final ResourceLocation NAUTILUS = texture("nautilus");
    private static final ResourceLocation BABY = texture("nautilus_baby");
    private static final ResourceLocation ZOMBIE = texture("zombie_nautilus");
    private static final ResourceLocation CORAL = texture("zombie_nautilus_coral");
    private static final ResourceLocation SADDLE = FutureBackport.id("textures/entity/equipment/nautilus_saddle/saddle.png");
    private final NautilusModel adult;
    private final NautilusModel baby;
    private final NautilusModel coral;

    public NautilusRenderer(EntityRendererProvider.Context context) {
        super(context, new NautilusModel(context.bakeLayer(ModModelLayers.NAUTILUS)), 0.5F);
        this.adult = getModel();
        this.baby = new NautilusModel(context.bakeLayer(ModModelLayers.NAUTILUS_BABY));
        this.coral = new NautilusModel(context.bakeLayer(ModModelLayers.ZOMBIE_NAUTILUS_CORAL));
        addLayer(new EquipmentLayer(this, new NautilusModel(context.bakeLayer(ModModelLayers.NAUTILUS_ARMOR)),
                new NautilusModel(context.bakeLayer(ModModelLayers.NAUTILUS_SADDLE))));
    }

    private static ResourceLocation texture(String name) {
        return FutureBackport.id("textures/entity/nautilus/" + name + ".png");
    }

    @Override
    public void render(AbstractNautilus nautilus, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
        model = nautilus.isBaby() ? baby : nautilus instanceof ZombieNautilus zombie && zombie.isCoral() ? coral : adult;
        super.render(nautilus, entityYaw, partialTicks, poseStack, buffer, light);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractNautilus nautilus) {
        if (nautilus instanceof ZombieNautilus zombie) return zombie.isCoral() ? CORAL : ZOMBIE;
        return nautilus.isBaby() ? BABY : NAUTILUS;
    }

    private static final class EquipmentLayer extends RenderLayer<AbstractNautilus, NautilusModel> {
        private final NautilusModel armor;
        private final NautilusModel saddle;

        EquipmentLayer(RenderLayerParent<AbstractNautilus, NautilusModel> parent, NautilusModel armor, NautilusModel saddle) {
            super(parent);
            this.armor = armor;
            this.saddle = saddle;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int light, AbstractNautilus nautilus, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (nautilus.isBaby()) return;
            int overlay = LivingEntityRenderer.getOverlayCoords(nautilus, 0.0F);
            if (nautilus.getBodyArmorItem().getItem() instanceof NautilusArmorItem item) {
                armor.setupAnim(nautilus, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                armor.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(item.texture())), light, overlay);
            }
            if (nautilus.isSaddled()) {
                saddle.setupAnim(nautilus, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
                saddle.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(SADDLE)), light, overlay);
            }
        }
    }
}
