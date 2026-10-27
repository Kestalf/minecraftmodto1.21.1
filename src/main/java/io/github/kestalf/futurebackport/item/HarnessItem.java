package io.github.kestalf.futurebackport.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

/** A harness (1.21.6): worn by happy ghasts in the body slot, which makes them rideable. */
public class HarnessItem extends Item {
    private final DyeColor color;

    public HarnessItem(DyeColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public DyeColor color() {
        return color;
    }
}
