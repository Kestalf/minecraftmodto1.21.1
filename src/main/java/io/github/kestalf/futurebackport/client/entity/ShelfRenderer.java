package io.github.kestalf.futurebackport.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kestalf.futurebackport.block.ShelfBlock;
import io.github.kestalf.futurebackport.block.entity.ShelfBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the three stored items in front of the shelf's back panel, left to right as seen from the front. */
public class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity> {
    private static final float SLOT_WIDTH = 1.0F / 3.0F;
    /** Items hang between the top and bottom boards, in front of the back panel (12/16 deep for a north-facing shelf). */
    private static final float DEPTH = 0.25F;
    /** A flat item fills about a third of the shelf's width. */
    private static final float SCALE = 0.25F;
    private final ItemRenderer itemRenderer;

    public ShelfRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ShelfBlockEntity shelf, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        Direction facing = shelf.getBlockState().getValue(ShelfBlock.FACING);
        int frontLight = shelf.getLevel() != null ? LevelRenderer.getLightColor(shelf.getLevel(), shelf.getBlockPos().relative(facing)) : light;
        for (int slot = 0; slot < ShelfBlockEntity.SLOTS; slot++) {
            ItemStack stack = shelf.getItem(slot);
            if (stack.isEmpty()) continue;
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
            // For a north-facing shelf the viewer's left is +x.
            poseStack.translate((1 - slot) * SLOT_WIDTH, 0.0F, DEPTH);
            poseStack.scale(SCALE * 2, SCALE * 2, SCALE * 2);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, frontLight, overlay, poseStack, buffer, shelf.getLevel(),
                    (int) shelf.getBlockPos().asLong() + slot);
            poseStack.popPose();
        }
    }
}
