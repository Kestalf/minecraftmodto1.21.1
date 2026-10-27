package io.github.kestalf.futurebackport.client;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.client.particle.FallingLeafParticle;
import io.github.kestalf.futurebackport.client.particle.FireflyParticle;
import io.github.kestalf.futurebackport.client.particle.GrowthParticle;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModParticles;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = FutureBackport.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (WoodFamily family : List.of(ModBlocks.PALE_OAK, ModBlocks.POPLAR)) {
                Sheets.addWoodType(family.woodType);
            }
            setRenderTypes();
        });
    }

    @SuppressWarnings("deprecation")
    private static void setRenderTypes() {
        List<DeferredBlock<? extends Block>> cutout = new ArrayList<>();
        for (WoodFamily family : List.of(ModBlocks.PALE_OAK, ModBlocks.POPLAR)) {
            cutout.addAll(List.of(family.door, family.trapdoor, family.sapling, family.pottedSapling));
            family.leaves.forEach(leaves -> ItemBlockRenderTypes.setRenderLayer(leaves.get(), RenderType.cutoutMipped()));
        }
        cutout.addAll(List.of(ModBlocks.PALE_MOSS_CARPET, ModBlocks.PALE_HANGING_MOSS, ModBlocks.OPEN_EYEBLOSSOM, ModBlocks.CLOSED_EYEBLOSSOM,
                ModBlocks.POTTED_OPEN_EYEBLOSSOM, ModBlocks.POTTED_CLOSED_EYEBLOSSOM, ModBlocks.RESIN_CLUMP, ModBlocks.LEAF_LITTER,
                ModBlocks.WILDFLOWERS, ModBlocks.BUSH, ModBlocks.FIREFLY_BUSH, ModBlocks.SHORT_DRY_GRASS, ModBlocks.TALL_DRY_GRASS,
                ModBlocks.CACTUS_FLOWER, ModBlocks.GOLDEN_DANDELION, ModBlocks.POTTED_GOLDEN_DANDELION, ModBlocks.RED_SHRUB,
                ModBlocks.SHELF_MUSHROOM));
        cutout.addAll(ModBlocks.COPPER.lanterns.all());
        cutout.add(ModBlocks.COPPER.torch);
        cutout.add(ModBlocks.COPPER.wallTorch);
        cutout.forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout()));
        ModBlocks.COPPER.bars.all().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutoutMipped()));
        ModBlocks.COPPER.chains.all().forEach(block -> ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutoutMipped()));
    }

    @SubscribeEvent
    static void onParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.COPPER_FIRE_FLAME, FlameParticle.Provider::new);
        event.registerSpriteSet(ModParticles.PALE_OAK_LEAVES, FallingLeafParticle.Provider::new);
        event.registerSpriteSet(ModParticles.RED_POPLAR_LEAVES, FallingLeafParticle.Provider::new);
        event.registerSpriteSet(ModParticles.ORANGE_POPLAR_LEAVES, FallingLeafParticle.Provider::new);
        event.registerSpriteSet(ModParticles.YELLOW_POPLAR_LEAVES, FallingLeafParticle.Provider::new);
        event.registerSpriteSet(ModParticles.FIREFLY, FireflyParticle.Provider::new);
        event.registerSpriteSet(ModParticles.PAUSE_MOB_GROWTH, sprites -> new GrowthParticle.Provider(sprites, -0.04));
        event.registerSpriteSet(ModParticles.RESET_MOB_GROWTH, sprites -> new GrowthParticle.Provider(sprites, 0.04));
    }

    @SubscribeEvent
    static void onReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new DryFoliageColor.ReloadListener());
    }

    @SubscribeEvent
    static void onColorResolvers(RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(DryFoliageColor.RESOLVER);
    }

    @SubscribeEvent
    static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        var colors = event.getBlockColors();
        event.register((state, level, pos, tint) -> level != null && pos != null
                ? BiomeColors.getAverageGrassColor(level, pos) : GrassColor.getDefaultColor(), ModBlocks.BUSH.get());
        event.register((state, level, pos, tint) -> colors.getColor(Blocks.PINK_PETALS.defaultBlockState(), level, pos, tint),
                ModBlocks.WILDFLOWERS.get());
        event.register((state, level, pos, tint) -> level != null && pos != null
                ? level.getBlockTint(pos, DryFoliageColor.RESOLVER) : DryFoliageColor.DEFAULT, ModBlocks.LEAF_LITTER.get());
    }

    @SubscribeEvent
    static void onItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> GrassColor.get(0.5, 1.0), ModBlocks.BUSH.get());
    }

    private ClientSetup() {
    }
}
