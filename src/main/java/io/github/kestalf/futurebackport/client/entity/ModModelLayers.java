package io.github.kestalf.futurebackport.client.entity;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModModelLayers {
    public static final ModelLayerLocation HAPPY_GHAST = layer("happy_ghast");
    public static final ModelLayerLocation HAPPY_GHAST_HARNESS = layer("happy_ghast", "harness");
    public static final ModelLayerLocation HAPPY_GHAST_ROPES = layer("happy_ghast", "ropes");
    public static final ModelLayerLocation CREAKING = layer("creaking");

    private static ModelLayerLocation layer(String name) {
        return layer(name, "main");
    }

    private static ModelLayerLocation layer(String name, String layer) {
        return new ModelLayerLocation(FutureBackport.id(name), layer);
    }

    private ModModelLayers() {
    }
}
