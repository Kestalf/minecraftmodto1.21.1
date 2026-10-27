package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.variant.FarmAnimalVariant;
import net.minecraft.client.model.ChickenModel;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Pig;

/**
 * Renderers replacing the vanilla chicken, cow and pig ones: temperate animals look exactly as before, warm and cold
 * ones use the 1.21.5 textures and models. The pig and cow models of 1.21.5 have 64x64 textures with fur overlays (a
 * transparent area on the other variants); the pig overlay comes from the official Bedrock "pig.v3" geometry, the
 * cold chicken's head tuft and tail and the cow's fur, tuft and horns are laid out after the texture.
 */
public final class FarmVariantRenderers {
    private static final CubeDeformation FUR = new CubeDeformation(0.5F);

    private static ResourceLocation texture(String mob, FarmAnimalVariant variant) {
        return FutureBackport.id("textures/entity/" + mob + "/" + mob + "_" + variant.getSerializedName() + ".png");
    }

    public static class Chickens extends ChickenRenderer {
        private final ChickenModel<Chicken> temperate;
        private final ChickenModel<Chicken> cold;

        public Chickens(EntityRendererProvider.Context context) {
            super(context);
            this.temperate = getModel();
            this.cold = new ChickenModel<>(context.bakeLayer(ModModelLayers.COLD_CHICKEN));
        }

        @Override
        public void render(Chicken chicken, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
            model = FarmAnimalVariant.of(chicken) == FarmAnimalVariant.COLD ? cold : temperate;
            super.render(chicken, entityYaw, partialTicks, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Chicken chicken) {
            FarmAnimalVariant variant = FarmAnimalVariant.of(chicken);
            return variant == FarmAnimalVariant.TEMPERATE ? super.getTextureLocation(chicken) : texture("chicken", variant);
        }
    }

    public static class Cows extends CowRenderer {
        private final CowModel<Cow> temperate;
        private final CowModel<Cow> variant;

        public Cows(EntityRendererProvider.Context context) {
            super(context);
            this.temperate = getModel();
            this.variant = new CowModel<>(context.bakeLayer(ModModelLayers.VARIANT_COW));
        }

        @Override
        public void render(Cow cow, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
            model = FarmAnimalVariant.of(cow) == FarmAnimalVariant.TEMPERATE ? temperate : variant;
            super.render(cow, entityYaw, partialTicks, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Cow cow) {
            FarmAnimalVariant variant = FarmAnimalVariant.of(cow);
            return variant == FarmAnimalVariant.TEMPERATE ? super.getTextureLocation(cow) : texture("cow", variant);
        }
    }

    public static class Pigs extends PigRenderer {
        private final PigModel<Pig> temperate;
        private final PigModel<Pig> variant;

        public Pigs(EntityRendererProvider.Context context) {
            super(context);
            this.temperate = getModel();
            this.variant = new PigModel<>(context.bakeLayer(ModModelLayers.VARIANT_PIG));
        }

        @Override
        public void render(Pig pig, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int light) {
            model = FarmAnimalVariant.of(pig) == FarmAnimalVariant.TEMPERATE ? temperate : variant;
            super.render(pig, entityYaw, partialTicks, poseStack, buffer, light);
        }

        @Override
        public ResourceLocation getTextureLocation(Pig pig) {
            FarmAnimalVariant variant = FarmAnimalVariant.of(pig);
            return variant == FarmAnimalVariant.TEMPERATE ? super.getTextureLocation(pig) : texture("pig", variant);
        }
    }

    /** The vanilla chicken plus the cold chicken's head tuft (6x3x4) and tail feathers. */
    public static LayerDefinition createColdChickenLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -6.0F, -2.0F, 4.0F, 6.0F, 3.0F)
                .texOffs(44, 0).addBox(-3.0F, -7.0F, -2.015F, 6.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 15.0F, -4.0F));
        root.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(14, 0).addBox(-2.0F, -4.0F, -4.0F, 4.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 15.0F, -4.0F));
        root.addOrReplaceChild("red_thing", CubeListBuilder.create().texOffs(14, 4).addBox(-1.0F, -2.0F, -3.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 15.0F, -4.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 9).addBox(-3.0F, -4.0F, -3.0F, 6.0F, 8.0F, 6.0F)
                        .texOffs(38, 9).addBox(0.0F, 3.0F, -1.0F, 0.0F, 3.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 16.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(26, 0).addBox(-1.0F, 0.0F, -3.0F, 3.0F, 5.0F, 3.0F);
        root.addOrReplaceChild("right_leg", leg, PartPose.offset(-2.0F, 19.0F, 1.0F));
        root.addOrReplaceChild("left_leg", leg, PartPose.offset(1.0F, 19.0F, 1.0F));
        root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(24, 13).addBox(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F),
                PartPose.offset(-4.0F, 13.0F, 0.0F));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(24, 13).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F),
                PartPose.offset(4.0F, 13.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * The 1.21.5 cow (muzzle, horns further forward) on a 64x64 texture, with the cold cow's fur, tuft and long horns
     * and the warm cow's big horns; each variant's texture leaves the other one's parts transparent.
     */
    public static LayerDefinition createVariantCowLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 6.0F)
                .texOffs(1, 33).addBox(-3.0F, 1.0F, -7.0F, 6.0F, 3.0F, 1.0F)
                .texOffs(22, 0).addBox("right_horn", -5.0F, -5.0F, -5.0F, 1.0F, 3.0F, 1.0F)
                .texOffs(22, 0).addBox("left_horn", 4.0F, -5.0F, -5.0F, 1.0F, 3.0F, 1.0F)
                .texOffs(6, 32).addBox(-3.0F, -5.0F, -6.0F, 6.0F, 3.0F, 4.0F), PartPose.offset(0.0F, 4.0F, -8.0F));
        // Warm cow: horns reaching out sideways and curling up.
        head.addOrReplaceChild("warm_horns", CubeListBuilder.create()
                .texOffs(27, 0).addBox(-8.0F, -4.0F, -5.0F, 4.0F, 2.0F, 2.0F)
                .texOffs(39, 0).addBox(-8.0F, -6.0F, -5.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(27, 0).mirror().addBox(4.0F, -4.0F, -5.0F, 4.0F, 2.0F, 2.0F)
                .texOffs(39, 0).addBox(6.0F, -6.0F, -5.0F, 2.0F, 2.0F, 2.0F), PartPose.ZERO);
        // Cold cow: long horns pointing sideways.
        head.addOrReplaceChild("cold_right_horn", CubeListBuilder.create().texOffs(0, 32).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, -3.0F, -4.0F, 0.0F, 0.0F, (float) (-Math.PI / 2)));
        head.addOrReplaceChild("cold_left_horn", CubeListBuilder.create().texOffs(0, 40).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, -3.0F, -4.0F, 0.0F, 0.0F, (float) (Math.PI / 2)));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(18, 4).addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F)
                        .texOffs(52, 0).addBox(-2.0F, 2.0F, -8.0F, 4.0F, 6.0F, 1.0F)
                        .texOffs(20, 32).addBox(-6.0F, -10.0F, -7.0F, 12.0F, 18.0F, 10.0F, FUR),
                PartPose.offsetAndRotation(0.0F, 5.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-4.0F, 12.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(4.0F, 12.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-4.0F, 12.0F, -6.0F));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(4.0F, 12.0F, -6.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    /** The pig on a 64x64 texture with the cold pig's fur (official Bedrock "pig.v3" geometry). */
    public static LayerDefinition createVariantPigLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -4.0F, -8.0F, 8.0F, 8.0F, 8.0F)
                .texOffs(16, 16).addBox(-2.0F, 0.0F, -9.0F, 4.0F, 3.0F, 1.0F), PartPose.offset(0.0F, 12.0F, -6.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(28, 8).addBox(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F)
                        .texOffs(28, 32).addBox(-5.0F, -10.0F, -7.0F, 10.0F, 16.0F, 8.0F, FUR),
                PartPose.offsetAndRotation(0.0F, 11.0F, 2.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F);
        root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-3.0F, 18.0F, 7.0F));
        root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(3.0F, 18.0F, 7.0F));
        root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-3.0F, 18.0F, -5.0F));
        root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(3.0F, 18.0F, -5.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    private FarmVariantRenderers() {
    }
}
