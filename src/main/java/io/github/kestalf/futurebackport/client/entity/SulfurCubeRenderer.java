package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.sulfur.SulfurCube;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Sulfur cube: an opaque core inside a translucent shell, like a slime. A cube with a block shows the block in place
 * of the core and no longer squishes; a lit explosive cube flashes and swells like TNT.
 */
public class SulfurCubeRenderer extends MobRenderer<SulfurCube, SulfurCubeRenderer.CubeModel> {
    private static final ResourceLocation INNER = texture("sulfur_cube_inner");
    private static final ResourceLocation INNER_SMALL = texture("sulfur_cube_inner_small");
    private static final ResourceLocation OUTER = texture("sulfur_cube_outer");
    private static final ResourceLocation OUTER_SMALL = texture("sulfur_cube_outer_small");
    private final CubeModel large;
    private final CubeModel small;

    public SulfurCubeRenderer(EntityRendererProvider.Context context) {
        super(context, new CubeModel(context.bakeLayer(ModModelLayers.SULFUR_CUBE)), 0.5F);
        this.large = model;
        this.small = new CubeModel(context.bakeLayer(ModModelLayers.SULFUR_CUBE_SMALL));
        addLayer(new BlockLayer(this, context.getBlockRenderDispatcher()));
        addLayer(new ShellLayer(this, new CubeModel(context.bakeLayer(ModModelLayers.SULFUR_CUBE_OUTER)),
                new CubeModel(context.bakeLayer(ModModelLayers.SULFUR_CUBE_SMALL_OUTER))));
    }

    private static ResourceLocation texture(String name) {
        return FutureBackport.id("textures/entity/sulfur_cube/" + name + ".png");
    }

    public static LayerDefinition createInnerLayer() {
        return box(0, 36, -8.0F, 7.0F, 16, -0.05F, 128);
    }

    public static LayerDefinition createOuterLayer() {
        return box(0, 0, -9.0F, 6.0F, 18, 0.0F, 128);
    }

    public static LayerDefinition createSmallInnerLayer() {
        return box(0, 20, -4.0F, 15.0F, 8, -0.05F, 64);
    }

    public static LayerDefinition createSmallOuterLayer() {
        return box(0, 0, -5.0F, 14.0F, 10, 0.0F, 64);
    }

    private static LayerDefinition box(int u, int v, float xz, float y, int size, float inflate, int textureSize) {
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().texOffs(u, v)
                .addBox(xz, y, xz, size, size, size, new CubeDeformation(inflate)), PartPose.ZERO);
        return LayerDefinition.create(mesh, textureSize, textureSize);
    }

    @Override
    public void render(SulfurCube cube, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        model = cube.isSmall() ? small : large;
        shadowRadius = 0.25F * cube.getSize();
        super.render(cube, yaw, partialTick, pose, buffers, light);
    }

    @Override
    @Nullable
    protected RenderType getRenderType(SulfurCube cube, boolean bodyVisible, boolean translucent, boolean glowing) {
        return cube.hasBlock() ? null : super.getRenderType(cube, bodyVisible, translucent, glowing);
    }

    @Override
    protected void scale(SulfurCube cube, PoseStack pose, float partialTick) {
        pose.scale(0.999F, 0.999F, 0.999F);
        pose.translate(0.0F, 0.001F, 0.0F);
        if (!cube.hasBlock()) {
            float squish = Mth.lerp(partialTick, cube.oSquish, cube.squish) / (cube.getSize() * 0.5F + 1.0F);
            float f = 1.0F / (squish + 1.0F);
            pose.scale(f, 1.0F / f, f);
        }
        if (cube.isIgnited() && cube.getFuse() - partialTick + 1.0F < 10.0F) {
            float f = Mth.clamp(1.0F - (cube.getFuse() - partialTick + 1.0F) / 10.0F, 0.0F, 1.0F);
            f *= f;
            f *= f;
            float swell = 1.0F + f * 0.3F;
            pose.scale(swell, swell, swell);
        }
    }

    @Override
    protected float getWhiteOverlayProgress(SulfurCube cube, float partialTicks) {
        return flash(cube);
    }

    static float flash(SulfurCube cube) {
        return cube.isIgnited() && cube.getFuse() / 5 % 2 == 0 ? 1.0F : 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(SulfurCube cube) {
        return cube.isSmall() ? INNER_SMALL : INNER;
    }

    public static class CubeModel extends HierarchicalModel<SulfurCube> {
        private final ModelPart root;

        public CubeModel(ModelPart root) {
            this.root = root;
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(SulfurCube cube, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        }
    }

    /** The absorbed block, one pixel above the ground in the middle of the shell; light sources glow. */
    static class BlockLayer extends RenderLayer<SulfurCube, CubeModel> {
        private final BlockRenderDispatcher blocks;

        BlockLayer(RenderLayerParent<SulfurCube, CubeModel> parent, BlockRenderDispatcher blocks) {
            super(parent);
            this.blocks = blocks;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, SulfurCube cube, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (!(cube.getBlock().getItem() instanceof BlockItem item)) return;
            BlockState state = item.getBlock().defaultBlockState();
            int emission = state.getLightEmission();
            int blockLight = emission > 0 ? LightTexture.pack(Math.max(LightTexture.block(light), emission), LightTexture.sky(light)) : light;
            int overlay = OverlayTexture.pack(OverlayTexture.u(flash(cube)), OverlayTexture.v(cube.hurtTime > 0 || cube.deathTime > 0));
            pose.pushPose();
            // Back from model space (y down, origin 1.5 blocks up) to the entity's own space.
            pose.translate(0.0F, 1.501F, 0.0F);
            pose.scale(-1.0F, -1.0F, 1.0F);
            pose.translate(-0.5F, 1.0F / 16.0F, -0.5F);
            blocks.renderSingleBlock(state, pose, buffers, blockLight, overlay);
            pose.popPose();
        }
    }

    /** The translucent outer shell with the face. */
    static class ShellLayer extends RenderLayer<SulfurCube, CubeModel> {
        private final CubeModel large;
        private final CubeModel small;

        ShellLayer(RenderLayerParent<SulfurCube, CubeModel> parent, CubeModel large, CubeModel small) {
            super(parent);
            this.large = large;
            this.small = small;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int light, SulfurCube cube, float limbSwing, float limbSwingAmount,
                           float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            boolean outlineOnly = Minecraft.getInstance().shouldEntityAppearGlowing(cube) && cube.isInvisible();
            if (cube.isInvisible() && !outlineOnly) return;
            ResourceLocation texture = cube.isSmall() ? OUTER_SMALL : OUTER;
            VertexConsumer consumer = buffers.getBuffer(outlineOnly ? RenderType.outline(texture) : RenderType.entityTranslucent(texture));
            CubeModel model = cube.isSmall() ? small : large;
            getParentModel().copyPropertiesTo(model);
            model.renderToBuffer(pose, consumer, light, LivingEntityRenderer.getOverlayCoords(cube, flash(cube)));
        }
    }
}
