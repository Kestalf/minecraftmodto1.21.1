package io.github.kestalf.futurebackport;

import com.mojang.logging.LogUtils;
import io.github.kestalf.futurebackport.network.ModNetwork;
import io.github.kestalf.futurebackport.registry.ModArmorMaterials;
import io.github.kestalf.futurebackport.registry.ModAttachments;
import io.github.kestalf.futurebackport.registry.ModBlockEntities;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModCreativeTabs;
import io.github.kestalf.futurebackport.registry.ModEffects;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModItems;
import io.github.kestalf.futurebackport.registry.ModParticles;
import io.github.kestalf.futurebackport.registry.ModRecipes;
import io.github.kestalf.futurebackport.registry.ModSounds;
import io.github.kestalf.futurebackport.worldgen.ModWorldgen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(FutureBackport.MOD_ID)
public final class FutureBackport {
    public static final String MOD_ID = "futurebackport";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FutureBackport(IEventBus modBus, ModContainer container) {
        ModSounds.SOUNDS.register(modBus);
        ModSounds.registerImported();
        ModBlocks.BLOCKS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModArmorMaterials.MATERIALS.register(modBus);
        ModRecipes.SERIALIZERS.register(modBus);
        ModParticles.PARTICLES.register(modBus);
        ModAttachments.ATTACHMENT_TYPES.register(modBus);
        ModCreativeTabs.TABS.register(modBus);
        ModWorldgen.register(modBus);

        modBus.addListener(CommonSetup::onCommonSetup);
        modBus.addListener(CommonSetup::onBlockEntityTypeAddBlocks);
        modBus.addListener(CommonSetup::onBuildCreativeTabs);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModEntities::registerSpawnPlacements);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
