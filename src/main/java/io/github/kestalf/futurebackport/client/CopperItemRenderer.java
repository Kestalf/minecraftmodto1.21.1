package io.github.kestalf.futurebackport.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kestalf.futurebackport.block.CopperChestBlock;
import io.github.kestalf.futurebackport.block.CopperGolemStatueBlock;
import io.github.kestalf.futurebackport.block.entity.CopperChestBlockEntity;
import io.github.kestalf.futurebackport.client.entity.CopperGolemStatueRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.HashMap;
import java.util.Map;

/** Item rendering of copper chests and copper golem statues, which have no block model. */
public class CopperItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final IClientItemExtensions EXTENSIONS = new IClientItemExtensions() {
        private CopperItemRenderer renderer;

        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
            if (renderer == null) renderer = new CopperItemRenderer();
            return renderer;
        }
    };

    private final Map<Block, CopperChestBlockEntity> chests = new HashMap<>();
    private CopperGolemStatueRenderer statues;

    private CopperItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        statues = new CopperGolemStatueRenderer(Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem item)) return;
        Block block = item.getBlock();
        if (block instanceof CopperChestBlock) {
            CopperChestBlockEntity chest = chests.computeIfAbsent(block, b -> new CopperChestBlockEntity(BlockPos.ZERO, b.defaultBlockState()));
            Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(chest, poseStack, buffer, light, overlay);
        } else if (block instanceof CopperGolemStatueBlock) {
            if (statues == null) statues = new CopperGolemStatueRenderer(Minecraft.getInstance().getEntityModels());
            BlockState state = stack.getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY).apply(block.defaultBlockState());
            statues.render(state, poseStack, buffer, light, overlay);
        }
    }
}
