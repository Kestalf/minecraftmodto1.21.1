package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.item.spear.SpearItem;
import io.github.kestalf.futurebackport.item.spear.SpearMaterial;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AnimalArmorItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BannerPatternItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FutureBackport.MOD_ID);

    // ------------------------------------------------------------------ materials

    public static final DeferredItem<Item> RESIN_BRICK = ITEMS.registerSimpleItem("resin_brick");
    public static final DeferredItem<Item> COPPER_NUGGET = ITEMS.registerSimpleItem("copper_nugget");

    // ------------------------------------------------------------------ copper tools and armor (1.21.9)

    /** Copper tools mine like stone tools but are faster and more durable. */
    public static final Tier COPPER_TIER = new SimpleTier(BlockTags.INCORRECT_FOR_STONE_TOOL, 190, 5.0F, 1.0F, 13,
            () -> Ingredient.of(Items.COPPER_INGOT));

    public static final DeferredItem<SwordItem> COPPER_SWORD = ITEMS.register("copper_sword",
            () -> new SwordItem(COPPER_TIER, new Item.Properties().attributes(SwordItem.createAttributes(COPPER_TIER, 3, -2.4F))));
    public static final DeferredItem<ShovelItem> COPPER_SHOVEL = ITEMS.register("copper_shovel",
            () -> new ShovelItem(COPPER_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(COPPER_TIER, 1.5F, -3.0F))));
    public static final DeferredItem<PickaxeItem> COPPER_PICKAXE = ITEMS.register("copper_pickaxe",
            () -> new PickaxeItem(COPPER_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(COPPER_TIER, 1.0F, -2.8F))));
    public static final DeferredItem<AxeItem> COPPER_AXE = ITEMS.register("copper_axe",
            () -> new AxeItem(COPPER_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(COPPER_TIER, 7.0F, -3.2F))));
    public static final DeferredItem<HoeItem> COPPER_HOE = ITEMS.register("copper_hoe",
            () -> new HoeItem(COPPER_TIER, new Item.Properties().attributes(DiggerItem.createAttributes(COPPER_TIER, -1.0F, -2.0F))));

    public static final DeferredItem<ArmorItem> COPPER_HELMET = armor("copper_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> COPPER_CHESTPLATE = armor("copper_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> COPPER_LEGGINGS = armor("copper_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> COPPER_BOOTS = armor("copper_boots", ArmorItem.Type.BOOTS);

    public static final DeferredItem<AnimalArmorItem> COPPER_HORSE_ARMOR = ITEMS.register("copper_horse_armor",
            () -> new AnimalArmorItem(ModArmorMaterials.COPPER, AnimalArmorItem.BodyType.EQUESTRIAN, false, new Item.Properties().stacksTo(1)));
    public static final DeferredItem<AnimalArmorItem> NETHERITE_HORSE_ARMOR = ITEMS.register("netherite_horse_armor",
            () -> new AnimalArmorItem(ModArmorMaterials.NETHERITE, AnimalArmorItem.BodyType.EQUESTRIAN, false,
                    new Item.Properties().stacksTo(1).fireResistant()));

    // ------------------------------------------------------------------ spears (1.21.11)

    public static final DeferredItem<SpearItem> WOODEN_SPEAR = spear(SpearMaterial.WOOD, new Item.Properties());
    public static final DeferredItem<SpearItem> STONE_SPEAR = spear(SpearMaterial.STONE, new Item.Properties());
    public static final DeferredItem<SpearItem> COPPER_SPEAR = spear(SpearMaterial.COPPER, new Item.Properties());
    public static final DeferredItem<SpearItem> IRON_SPEAR = spear(SpearMaterial.IRON, new Item.Properties());
    public static final DeferredItem<SpearItem> GOLDEN_SPEAR = spear(SpearMaterial.GOLD, new Item.Properties());
    public static final DeferredItem<SpearItem> DIAMOND_SPEAR = spear(SpearMaterial.DIAMOND, new Item.Properties());
    public static final DeferredItem<SpearItem> NETHERITE_SPEAR = spear(SpearMaterial.NETHERITE, new Item.Properties().fireResistant());
    public static final List<DeferredItem<SpearItem>> SPEARS = List.of(WOODEN_SPEAR, STONE_SPEAR, COPPER_SPEAR, IRON_SPEAR, GOLDEN_SPEAR,
            DIAMOND_SPEAR, NETHERITE_SPEAR);

    // ------------------------------------------------------------------ dyed items

    /** Harnesses (1.21.6) are equipped on happy ghasts to ride them. */
    public static final Map<DyeColor, DeferredItem<Item>> HARNESSES = new EnumMap<>(DyeColor.class);
    /** Dyed bundles (1.21.2). */
    public static final Map<DyeColor, DeferredItem<BundleItem>> BUNDLES = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            HARNESSES.put(color, ITEMS.register(color.getName() + "_harness",
                    () -> new io.github.kestalf.futurebackport.item.HarnessItem(color, new Item.Properties().stacksTo(1))));
        }
        for (DyeColor color : DyeColor.values()) {
            BUNDLES.put(color, ITEMS.register(color.getName() + "_bundle",
                    () -> new BundleItem(new Item.Properties().stacksTo(1).component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY))));
        }
    }

    // ------------------------------------------------------------------ misc

    public static final DeferredItem<Item> MUSIC_DISC_TEARS = disc("music_disc_tears", ModKeys.TEARS);
    public static final DeferredItem<Item> MUSIC_DISC_LAVA_CHICKEN = disc("music_disc_lava_chicken", ModKeys.LAVA_CHICKEN);
    public static final DeferredItem<Item> MUSIC_DISC_BOUNCE = disc("music_disc_bounce", ModKeys.BOUNCE);

    public static final DeferredItem<BannerPatternItem> FIELD_MASONED_BANNER_PATTERN = bannerPattern("field_masoned_banner_pattern",
            ModTags.BannerPatterns.FIELD_MASONED);
    public static final DeferredItem<BannerPatternItem> BORDURE_INDENTED_BANNER_PATTERN = bannerPattern("bordure_indented_banner_pattern",
            ModTags.BannerPatterns.BORDURE_INDENTED);

    // ------------------------------------------------------------------ spawn eggs (textures come from the newer client, so no tint)

    public static final DeferredItem<DeferredSpawnEggItem> HAPPY_GHAST_SPAWN_EGG = ITEMS.register("happy_ghast_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.HAPPY_GHAST, 0xFFFFFF, 0xFFFFFF, new Item.Properties()));

    // ------------------------------------------------------------------ helpers

    private static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(ModArmorMaterials.COPPER, type,
                new Item.Properties().durability(type.getDurability(11))));
    }

    private static DeferredItem<SpearItem> spear(SpearMaterial material, Item.Properties properties) {
        return ITEMS.register(material.name() + "_spear", () -> new SpearItem(material, properties));
    }

    private static DeferredItem<Item> disc(String name, ResourceKey<JukeboxSong> song) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).jukeboxPlayable(song)));
    }

    private static DeferredItem<BannerPatternItem> bannerPattern(String name, TagKey<BannerPattern> patterns) {
        return ITEMS.register(name, () -> new BannerPatternItem(patterns, new Item.Properties().stacksTo(1)));
    }

    private ModItems() {
    }
}
