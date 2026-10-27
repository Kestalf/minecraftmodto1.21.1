package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.List;

/**
 * Block states that cannot be taken from the newer client. Since 26.x signs are rendered from block models, while
 * 1.21.1 renders them with a block entity renderer and only needs a particle texture here.
 */
public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, FutureBackport.MOD_ID, helper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (WoodFamily f : List.of(ModBlocks.PALE_OAK, ModBlocks.POPLAR)) {
            ResourceLocation planks = FutureBackport.id("block/" + f.name + "_planks");
            ResourceLocation strippedLog = FutureBackport.id("block/stripped_" + f.name + "_log");
            particleOnly(f.sign.get(), f.name + "_sign_particle", planks);
            particleOnly(f.wallSign.get(), f.name + "_sign_particle", planks);
            particleOnly(f.hangingSign.get(), f.name + "_hanging_sign_particle", strippedLog);
            particleOnly(f.wallHangingSign.get(), f.name + "_hanging_sign_particle", strippedLog);
        }
    }

    private void particleOnly(Block block, String modelName, ResourceLocation texture) {
        ModelFile model = models().getBuilder("block/" + modelName).texture("particle", texture);
        simpleBlock(block, model);
    }
}
