package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FutureBackport.MOD_ID);

    public static final DeferredItem<Item> RESIN_BRICK = ITEMS.registerSimpleItem("resin_brick");
    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.registerSimpleItem("copper_nugget");

    private ModItems() {
    }
}
