package io.github.kestalf.futurebackport.registry;

import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;

import java.util.function.Supplier;

/** Boat types added through NeoForge's enum extension (see META-INF/enumextensions.json). */
public final class ModBoatTypes {
    public static final EnumProxy<Boat.Type> PALE_OAK = proxy("pale_oak", () -> ModBlocks.PALE_OAK.planks.get(),
            () -> ModBlocks.PALE_OAK.boat.get(), () -> ModBlocks.PALE_OAK.chestBoat.get());
    public static final EnumProxy<Boat.Type> POPLAR = proxy("poplar", () -> ModBlocks.POPLAR.planks.get(),
            () -> ModBlocks.POPLAR.boat.get(), () -> ModBlocks.POPLAR.chestBoat.get());

    private static EnumProxy<Boat.Type> proxy(String name, Supplier<Block> planks, Supplier<Item> boat, Supplier<Item> chestBoat) {
        return new EnumProxy<>(Boat.Type.class, planks, "futurebackport:" + name, boat, chestBoat, (Supplier<Item>) () -> Items.STICK, false);
    }

    private ModBoatTypes() {
    }
}
