package io.github.kestalf.futurebackport.client.entity;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModModelLayers {
    public static final ModelLayerLocation HAPPY_GHAST = layer("happy_ghast");
    public static final ModelLayerLocation HAPPY_GHAST_HARNESS = layer("happy_ghast", "harness");
    public static final ModelLayerLocation HAPPY_GHAST_ROPES = layer("happy_ghast", "ropes");
    public static final ModelLayerLocation CREAKING = layer("creaking");
    public static final ModelLayerLocation COPPER_GOLEM = layer("copper_golem");
    public static final ModelLayerLocation COPPER_GOLEM_SITTING = layer("copper_golem", "sitting");
    public static final ModelLayerLocation COPPER_GOLEM_RUNNING = layer("copper_golem", "running");
    public static final ModelLayerLocation COPPER_GOLEM_STAR = layer("copper_golem", "star");
    public static final ModelLayerLocation NAUTILUS = layer("nautilus");
    public static final ModelLayerLocation NAUTILUS_BABY = layer("nautilus_baby");
    public static final ModelLayerLocation NAUTILUS_SADDLE = layer("nautilus", "saddle");
    public static final ModelLayerLocation NAUTILUS_ARMOR = layer("nautilus", "armor");
    public static final ModelLayerLocation ZOMBIE_NAUTILUS_CORAL = layer("zombie_nautilus", "coral");

    private static ModelLayerLocation layer(String name) {
        return layer(name, "main");
    }

    private static ModelLayerLocation layer(String name, String layer) {
        return new ModelLayerLocation(FutureBackport.id(name), layer);
    }

    private ModModelLayers() {
    }
}
