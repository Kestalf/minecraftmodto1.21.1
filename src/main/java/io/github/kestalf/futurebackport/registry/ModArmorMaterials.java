package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, FutureBackport.MOD_ID);

    /** Copper armor (1.21.9) and copper horse armor. Textures: textures/models/armor/copper_layer_*.png. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> COPPER = MATERIALS.register("copper", () -> new ArmorMaterial(
            defense(1, 3, 4, 2, 4), 8, ModSounds.event("item.armor.equip_copper"), () -> Ingredient.of(Items.COPPER_INGOT),
            List.of(new ArmorMaterial.Layer(FutureBackport.id("copper"))), 0.0F, 0.0F));
    /** Netherite horse (and nautilus) armor (1.21.11). */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> NETHERITE = MATERIALS.register("netherite", () -> new ArmorMaterial(
            defense(3, 6, 8, 3, 19), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(Items.NETHERITE_INGOT),
            List.of(new ArmorMaterial.Layer(FutureBackport.id("netherite"))), 3.0F, 0.1F));

    private static Map<ArmorItem.Type, Integer> defense(int boots, int leggings, int chestplate, int helmet, int body) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.CHESTPLATE, chestplate);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.BODY, body);
        return map;
    }

    private ModArmorMaterials() {
    }
}
