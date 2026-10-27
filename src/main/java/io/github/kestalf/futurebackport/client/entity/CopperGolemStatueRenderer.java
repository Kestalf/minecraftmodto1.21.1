package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kestalf.futurebackport.block.CopperGolemPose;
import io.github.kestalf.futurebackport.block.CopperGolemStatueBlock;
import io.github.kestalf.futurebackport.block.entity.CopperGolemStatueBlockEntity;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

/** Draws a copper golem statue with the golem model of its pose and the texture of its oxidation stage. */
public class CopperGolemStatueRenderer implements BlockEntityRenderer<CopperGolemStatueBlockEntity> {
    private final Map<CopperGolemPose, CopperGolemModel<Entity>> models = new EnumMap<>(CopperGolemPose.class);

    public CopperGolemStatueRenderer(BlockEntityRendererProvider.Context context) {
        this(context.getModelSet());
    }

    public CopperGolemStatueRenderer(EntityModelSet modelSet) {
        models.put(CopperGolemPose.STANDING, new CopperGolemModel<>(modelSet.bakeLayer(ModModelLayers.COPPER_GOLEM)));
        models.put(CopperGolemPose.SITTING, new CopperGolemModel<>(modelSet.bakeLayer(ModModelLayers.COPPER_GOLEM_SITTING)));
        models.put(CopperGolemPose.RUNNING, new CopperGolemModel<>(modelSet.bakeLayer(ModModelLayers.COPPER_GOLEM_RUNNING)));
        models.put(CopperGolemPose.STAR, new CopperGolemModel<>(modelSet.bakeLayer(ModModelLayers.COPPER_GOLEM_STAR)));
    }

    @Override
    public void render(CopperGolemStatueBlockEntity statue, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        render(statue.getBlockState(), poseStack, buffer, light, overlay);
    }

    public void render(BlockState state, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        if (!(state.getBlock() instanceof CopperGolemStatueBlock block)) return;
        Direction facing = state.getValue(CopperGolemStatueBlock.FACING);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        CopperGolemModel<Entity> model = models.get(state.getValue(CopperGolemStatueBlock.POSE));
        model.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(CopperGolemRenderer.texture(block.getAge(), ""))),
                light, overlay);
        poseStack.popPose();
    }
}
