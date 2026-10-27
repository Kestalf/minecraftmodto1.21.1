package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final class Blocks {
        public static final TagKey<Block> PALE_OAK_LOGS = tag("pale_oak_logs");
        public static final TagKey<Block> POPLAR_LOGS = tag("poplar_logs");
        public static final TagKey<Block> LIGHTNING_RODS = tag("lightning_rods");
        public static final TagKey<Block> COPPER_CHESTS = tag("copper_chests");
        public static final TagKey<Block> WOODEN_SHELVES = tag("wooden_shelves");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, FutureBackport.id(name));
        }
    }

    public static final class Items {
        public static final TagKey<Item> PALE_OAK_LOGS = tag("pale_oak_logs");
        public static final TagKey<Item> POPLAR_LOGS = tag("poplar_logs");
        public static final TagKey<Item> HARNESSES = tag("harnesses");
        public static final TagKey<Item> SPEARS = tag("spears");
        public static final TagKey<Item> BUNDLES = tag("bundles");
        public static final TagKey<Item> HAPPY_GHAST_FOOD = tag("happy_ghast_food");
        public static final TagKey<Item> HAPPY_GHAST_TEMPT_ITEMS = tag("happy_ghast_tempt_items");
        public static final TagKey<Item> WOODEN_SHELVES = tag("wooden_shelves");
        public static final TagKey<Item> ENCHANTABLE_LUNGE = tag("enchantable/lunge");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, FutureBackport.id(name));
        }
    }

    public static final class Entities {
        public static final TagKey<EntityType<?>> CANNOT_BE_AGE_LOCKED = tag("cannot_be_age_locked");
        public static final TagKey<EntityType<?>> FOLLOWABLE_BY_GHASTLING = tag("followable_by_ghastling");
        public static final TagKey<EntityType<?>> CAN_BE_CARRIED_BY_HAPPY_GHAST = tag("happy_ghast_can_carry");

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, FutureBackport.id(name));
        }
    }

    public static final class BannerPatterns {
        public static final TagKey<net.minecraft.world.level.block.entity.BannerPattern> FIELD_MASONED = tag("pattern_item/field_masoned");
        public static final TagKey<net.minecraft.world.level.block.entity.BannerPattern> BORDURE_INDENTED = tag("pattern_item/bordure_indented");

        private static TagKey<net.minecraft.world.level.block.entity.BannerPattern> tag(String name) {
            return TagKey.create(Registries.BANNER_PATTERN, FutureBackport.id(name));
        }
    }

    private ModTags() {
    }
}
