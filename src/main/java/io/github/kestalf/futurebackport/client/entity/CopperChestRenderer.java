package io.github.kestalf.futurebackport.client.entity;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.block.CopperChestBlock;
import io.github.kestalf.futurebackport.block.entity.CopperChestBlockEntity;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.properties.ChestType;

import java.util.HashMap;
import java.util.Map;

/** The vanilla chest model with the copper textures ({@code entity/chest/copper[_<stage>][_left|_right]}). */
public class CopperChestRenderer extends ChestRenderer<CopperChestBlockEntity> {
    private static final Map<String, Material> MATERIALS = new HashMap<>();

    public CopperChestRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected Material getMaterial(CopperChestBlockEntity chest, ChestType type) {
        WeatherState stage = chest.getBlockState().getBlock() instanceof CopperChestBlock block ? block.getAge() : WeatherState.UNAFFECTED;
        String name = "copper" + (stage == WeatherState.UNAFFECTED ? "" : "_" + stage.getSerializedName())
                + (type == ChestType.LEFT ? "_left" : type == ChestType.RIGHT ? "_right" : "");
        return MATERIALS.computeIfAbsent(name, n -> new Material(Sheets.CHEST_SHEET, FutureBackport.id("entity/chest/" + n)));
    }
}
