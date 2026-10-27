package io.github.kestalf.futurebackport.item;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Body armor for nautiluses; the stats are those of the 1.21.11 items. */
public class NautilusArmorItem extends Item {
    private static final ResourceLocation MODIFIER_ID = ResourceLocation.withDefaultNamespace("armor.body");
    private final ResourceLocation texture;

    public NautilusArmorItem(String material, double armor, double toughness, double knockbackResistance, Properties properties) {
        super(properties.stacksTo(1).attributes(modifiers(armor, toughness, knockbackResistance)));
        this.texture = FutureBackport.id("textures/entity/equipment/nautilus_body/" + material + ".png");
    }

    private static ItemAttributeModifiers modifiers(double armor, double toughness, double knockbackResistance) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR, new AttributeModifier(MODIFIER_ID, armor, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.BODY)
                .add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(MODIFIER_ID, toughness, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.BODY);
        if (knockbackResistance > 0) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(MODIFIER_ID, knockbackResistance,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.BODY);
        }
        return builder.build();
    }

    public ResourceLocation texture() {
        return texture;
    }
}
