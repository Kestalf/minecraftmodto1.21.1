package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.loot.AddItemLootModifier;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModItems;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.advancements.critereon.EntityFlagsPredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The 26.3 additions to vanilla loot tables. Where 26.3 adds an entry to an existing weighted pool, the item gets its
 * own chance here: the probability that the pool's rolls pick it at least once ({@link #chance}).
 */
public class ModLootModifierProvider extends GlobalLootModifierProvider {
    public ModLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, FutureBackport.MOD_ID);
    }

    /** Chance that {@code minRolls..maxRolls} rolls of a pool with total weight {@code total} pick an entry of {@code weight}. */
    private static float chance(int minRolls, int maxRolls, int weight, int total) {
        double sum = 0.0;
        for (int rolls = minRolls; rolls <= maxRolls; rolls++) sum += 1.0 - Math.pow(1.0 - (double) weight / total, rolls);
        return (float) (sum / (maxRolls - minRolls + 1));
    }

    private static LootItemCondition[] conditions(String table, LootItemCondition... extra) {
        List<LootItemCondition> list = new ArrayList<>();
        list.add(LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace(table)).build());
        list.addAll(List.of(extra));
        return list.toArray(LootItemCondition[]::new);
    }

    private static LootItemCondition randomChance(float chance) {
        return LootItemRandomChanceCondition.randomChance(chance).build();
    }

    private void chest(String table, ItemLike item, float chance) {
        chest(table, item, chance, ConstantInt.of(1));
    }

    private void chest(String table, ItemLike item, float chance, IntProvider count) {
        String name = table.substring(table.lastIndexOf('/') + 1) + "_" + item.asItem().builtInRegistryHolder().key().location().getPath();
        add(name, AddItemLootModifier.of(conditions("chests/" + table, randomChance(chance)), item.asItem(), count));
    }

    private void nautilusArmor(String table) {
        chest(table, ModItems.COPPER_NAUTILUS_ARMOR.get(), 20 / 185.0F);
        chest(table, ModItems.IRON_NAUTILUS_ARMOR.get(), 10 / 185.0F);
        chest(table, ModItems.GOLDEN_NAUTILUS_ARMOR.get(), 5 / 185.0F);
        chest(table, ModItems.DIAMOND_NAUTILUS_ARMOR.get(), 2 / 185.0F);
    }

    @Override
    protected void start() {
        Item copperHorseArmor = ModItems.COPPER_HORSE_ARMOR.get();
        UniformInt leather = UniformInt.of(1, 5);

        chest("abandoned_mineshaft", ModItems.MUSIC_DISC_BOUNCE.get(), chance(3, 3, 10, 60));
        chest("ancient_city", Items.LEATHER, chance(5, 10, 2, 84), leather);
        add("bastion_treasure_diamond_spear", AddItemLootModifier.of(conditions("chests/bastion_treasure", randomChance(chance(3, 3, 12, 112))),
                ModItems.DIAMOND_SPEAR.get(), ConstantInt.of(1)).enchantedRandomly(0.8F));
        chest("buried_treasure", ModItems.IRON_SPEAR.get(), 0.5F / 3.0F);
        nautilusArmor("buried_treasure");
        chest("desert_pyramid", Items.LEATHER, chance(2, 4, 20, 247), leather);
        chest("desert_pyramid", copperHorseArmor, chance(2, 4, 15, 247));
        chest("end_city_treasure", copperHorseArmor, chance(2, 6, 1, 89));
        add("end_city_treasure_diamond_spear", AddItemLootModifier.of(conditions("chests/end_city_treasure", randomChance(chance(2, 6, 3, 89))),
                ModItems.DIAMOND_SPEAR.get(), ConstantInt.of(1)).enchantedWithLevels(UniformInt.of(20, 39)));
        chest("jungle_temple", Items.LEATHER, chance(2, 6, 3, 89), leather);
        chest("jungle_temple", copperHorseArmor, chance(2, 6, 1, 89));
        chest("nether_bridge", copperHorseArmor, chance(2, 4, 5, 78));
        chest("ruined_portal", Items.LODESTONE, 2 / 3.0F, UniformInt.of(1, 2));
        nautilusArmor("shipwreck_map");
        nautilusArmor("shipwreck_supply");
        nautilusArmor("shipwreck_treasure");
        chest("simple_dungeon", Items.LEATHER, chance(1, 3, 20, 144), leather);
        chest("simple_dungeon", copperHorseArmor, chance(1, 3, 15, 144));
        chest("stronghold_corridor", Items.LEATHER, chance(2, 3, 1, 101), leather);
        chest("stronghold_corridor", copperHorseArmor, chance(2, 3, 1, 101));
        chest("underwater_ruin_big", ModItems.STONE_SPEAR.get(), chance(2, 8, 2, 33));
        nautilusArmor("underwater_ruin_big");
        chest("underwater_ruin_small", ModItems.STONE_SPEAR.get(), chance(2, 8, 2, 30));
        nautilusArmor("underwater_ruin_small");
        chest("village/village_weaponsmith", ModItems.IRON_SPEAR.get(), chance(3, 8, 5, 107));
        chest("village/village_weaponsmith", ModItems.COPPER_SPEAR.get(), chance(3, 8, 7, 107));
        chest("village/village_weaponsmith", copperHorseArmor, chance(3, 8, 1, 107));
        chest("woodland_mansion", ModBlocks.RESIN_CLUMP.get(), chance(1, 4, 50, 175), UniformInt.of(2, 4));

        // Mob drops: Tears from a ghast killed with its own fireball, Lava Chicken from a chicken jockey,
        // a rabbit's foot from a husk riding a camel husk.
        add("ghast_music_disc_tears", AddItemLootModifier.of(conditions("entities/ghast",
                LootItemKilledByPlayerCondition.killedByPlayer().build(),
                DamageSourceCondition.hasDamageSource(DamageSourcePredicate.Builder.damageType()
                        .tag(TagPredicate.is(DamageTypeTags.IS_PROJECTILE)).direct(EntityPredicate.Builder.entity().of(EntityType.FIREBALL))).build()),
                ModItems.MUSIC_DISC_TEARS.get(), ConstantInt.of(1)));
        add("zombie_music_disc_lava_chicken", AddItemLootModifier.of(conditions("entities/zombie",
                LootItemKilledByPlayerCondition.killedByPlayer().build(),
                LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity()
                        .flags(EntityFlagsPredicate.Builder.flags().setIsBaby(true))
                        .vehicle(EntityPredicate.Builder.entity().of(EntityType.CHICKEN))).build()),
                ModItems.MUSIC_DISC_LAVA_CHICKEN.get(), ConstantInt.of(1)));
        add("husk_rabbit_foot", AddItemLootModifier.of(conditions("entities/husk",
                LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity()
                        .vehicle(EntityPredicate.Builder.entity().of(ModEntities.CAMEL_HUSK.get()))).build()),
                Items.RABBIT_FOOT, UniformInt.of(0, 1)));

        // Piglins sometimes barter a dried ghast instead of their usual goods (weight 10 of 469).
        add("piglin_bartering_dried_ghast", AddItemLootModifier.of(conditions("gameplay/piglin_bartering", randomChance(10 / 469.0F)),
                ModBlocks.DRIED_GHAST.get().asItem(), ConstantInt.of(1)).replacing());
    }
}
