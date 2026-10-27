package io.github.kestalf.futurebackport.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.List;
import java.util.Optional;

/**
 * Adds an item to the loot of the tables its conditions select (or, with {@code replace}, swaps the whole loot for
 * it). Used for the 26.3 additions to vanilla loot tables, which 1.21.1 data cannot express by editing the pools.
 */
public class AddItemLootModifier extends LootModifier {
    public static final MapCodec<AddItemLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance -> codecStart(instance).and(instance.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item),
            IntProvider.CODEC.optionalFieldOf("count", ConstantInt.of(1)).forGetter(m -> m.count),
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(m -> m.replace),
            Codec.BOOL.optionalFieldOf("enchant_randomly", false).forGetter(m -> m.enchantRandomly),
            IntProvider.CODEC.optionalFieldOf("enchant_levels").forGetter(m -> m.enchantLevels),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("min_durability", 1.0F).forGetter(m -> m.minDurability)
    )).apply(instance, AddItemLootModifier::new));

    private final Item item;
    private final IntProvider count;
    private final boolean replace;
    private final boolean enchantRandomly;
    private final Optional<IntProvider> enchantLevels;
    private final float minDurability;

    public AddItemLootModifier(LootItemCondition[] conditions, Item item, IntProvider count, boolean replace, boolean enchantRandomly,
                               Optional<IntProvider> enchantLevels, float minDurability) {
        super(conditions);
        this.item = item;
        this.count = count;
        this.replace = replace;
        this.enchantRandomly = enchantRandomly;
        this.enchantLevels = enchantLevels;
        this.minDurability = minDurability;
    }

    public static AddItemLootModifier of(LootItemCondition[] conditions, Item item, IntProvider count) {
        return new AddItemLootModifier(conditions, item, count, false, false, Optional.empty(), 1.0F);
    }

    public AddItemLootModifier replacing() {
        return new AddItemLootModifier(conditions, item, count, true, enchantRandomly, enchantLevels, minDurability);
    }

    public AddItemLootModifier enchantedRandomly(float minDurability) {
        return new AddItemLootModifier(conditions, item, count, replace, true, enchantLevels, minDurability);
    }

    public AddItemLootModifier enchantedWithLevels(IntProvider levels) {
        return new AddItemLootModifier(conditions, item, count, replace, enchantRandomly, Optional.of(levels), minDurability);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        RandomSource random = context.getRandom();
        int amount = count.sample(random);
        if (amount <= 0) return loot;
        ItemStack stack = new ItemStack(item, amount);
        var registries = context.getLevel().registryAccess();
        if (enchantRandomly) {
            List<Holder<Enchantment>> options = registries.registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.ON_RANDOM_LOOT)
                    .stream().flatMap(set -> set.stream()).filter(holder -> holder.value().canEnchant(stack)).toList();
            if (!options.isEmpty()) {
                Holder<Enchantment> enchantment = options.get(random.nextInt(options.size()));
                stack.enchant(enchantment, Mth.nextInt(random, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel()));
            }
        }
        enchantLevels.ifPresent(levels -> EnchantmentHelper.enchantItem(random, stack, levels.sample(random), registries,
                registries.registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.ON_RANDOM_LOOT)));
        if (minDurability < 1.0F && stack.isDamageableItem()) {
            float durability = Mth.nextFloat(random, minDurability, 1.0F);
            stack.setDamageValue(Mth.floor((1.0F - durability) * stack.getMaxDamage()));
        }
        if (replace) loot.clear();
        loot.add(stack);
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
