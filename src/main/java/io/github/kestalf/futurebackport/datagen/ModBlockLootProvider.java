package io.github.kestalf.futurebackport.datagen;

import io.github.kestalf.futurebackport.block.StoneFamily;
import io.github.kestalf.futurebackport.block.WoodFamily;
import io.github.kestalf.futurebackport.block.plant.LeafLitterBlock;
import io.github.kestalf.futurebackport.block.plant.PaleMossCarpetBlock;
import io.github.kestalf.futurebackport.block.plant.ShelfMushroomBlock;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

public class ModBlockLootProvider extends BlockLootSubProvider {
    public ModBlockLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (WoodFamily f : List.of(ModBlocks.PALE_OAK, ModBlocks.POPLAR)) {
            for (Block b : List.of(f.log.get(), f.wood.get(), f.strippedLog.get(), f.strippedWood.get(), f.planks.get(), f.stairs.get(),
                    f.fence.get(), f.fenceGate.get(), f.trapdoor.get(), f.button.get(), f.pressurePlate.get(), f.sign.get(),
                    f.hangingSign.get(), f.sapling.get())) {
                dropSelf(b);
            }
            add(f.slab.get(), this::createSlabItemTable);
            add(f.door.get(), this::createDoorTable);
            dropPottedContents(f.pottedSapling.get());
            f.leaves.forEach(leaves -> add(leaves.get(), block -> createLeavesDrops(block, f.sapling.get(), NORMAL_LEAVES_SAPLING_CHANCES)));
        }
        for (StoneFamily family : List.of(ModBlocks.RESIN_BRICKS, ModBlocks.CINNABAR, ModBlocks.POLISHED_CINNABAR, ModBlocks.CINNABAR_BRICKS,
                ModBlocks.SULFUR, ModBlocks.POLISHED_SULFUR, ModBlocks.SULFUR_BRICKS)) {
            dropSelf(family.base().get());
            dropSelf(family.stairs().get());
            dropSelf(family.wall().get());
            add(family.slab().get(), this::createSlabItemTable);
        }
        dropSelf(ModBlocks.DRIED_GHAST.get());
        var fortune = registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        add(ModBlocks.CREAKING_HEART.get(), block -> createSilkTouchDispatchTable(block, applyExplosionDecay(block,
                LootItem.lootTableItem(ModBlocks.RESIN_CLUMP.get())
                        .apply(SetItemCountFunction.setCount(net.minecraft.world.level.storage.loot.providers.number.UniformGenerator.between(1, 3)))
                        .apply(net.minecraft.world.level.storage.loot.functions.ApplyBonusCount.addUniformBonusCount(fortune))
                        .apply(net.minecraft.world.level.storage.loot.functions.LimitCount.limitCount(
                                net.minecraft.world.level.storage.loot.IntRange.upperBound(9))))));
        for (var b : List.of(ModBlocks.CHISELED_RESIN_BRICKS, ModBlocks.CHISELED_CINNABAR, ModBlocks.CHISELED_SULFUR, ModBlocks.RESIN_BLOCK,
                ModBlocks.PALE_MOSS_BLOCK)) {
            dropSelf(b.get());
        }
        ModBlocks.WOOL_STAIRS.values().forEach(b -> dropSelf(b.get()));
        ModBlocks.CONCRETE_STAIRS.values().forEach(b -> dropSelf(b.get()));
        ModBlocks.WOOL_SLABS.values().forEach(b -> add(b.get(), this::createSlabItemTable));
        ModBlocks.CONCRETE_SLABS.values().forEach(b -> add(b.get(), this::createSlabItemTable));

        ModBlocks.COPPER.chests.all().forEach(b -> add(b.get(), this::createNameableBlockEntityTable));
        ModBlocks.COPPER.statues.all().forEach(b -> add(b.get(), block -> LootTable.lootTable().withPool(applyExplosionCondition(block,
                LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(block)
                        .apply(net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
                                .copyComponents(net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction.Source.BLOCK_ENTITY)
                                .include(net.minecraft.core.component.DataComponents.CUSTOM_NAME))
                        .apply(net.minecraft.world.level.storage.loot.functions.CopyBlockState.copyState(block)
                                .copy(io.github.kestalf.futurebackport.block.CopperGolemStatueBlock.POSE)))))));
        ModBlocks.COPPER.all().forEach(b -> {
            if (b != ModBlocks.COPPER.wallTorch) dropSelf(b.get());
        });

        // Plants
        for (var b : List.of(ModBlocks.OPEN_EYEBLOSSOM, ModBlocks.CLOSED_EYEBLOSSOM, ModBlocks.GOLDEN_DANDELION, ModBlocks.BUSH,
                ModBlocks.FIREFLY_BUSH, ModBlocks.CACTUS_FLOWER, ModBlocks.RED_SHRUB)) {
            dropSelf(b.get());
        }
        for (DeferredHolder<Block, FlowerPotBlock> pot : List.of(ModBlocks.POTTED_OPEN_EYEBLOSSOM, ModBlocks.POTTED_CLOSED_EYEBLOSSOM,
                ModBlocks.POTTED_GOLDEN_DANDELION)) {
            dropPottedContents(pot.get());
        }
        add(ModBlocks.SHORT_DRY_GRASS.get(), createShearsOnlyDrop(ModBlocks.SHORT_DRY_GRASS.get()));
        add(ModBlocks.TALL_DRY_GRASS.get(), createShearsOnlyDrop(ModBlocks.TALL_DRY_GRASS.get()));
        add(ModBlocks.PALE_HANGING_MOSS.get(), block -> LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .when(HAS_SHEARS.or(hasSilkTouch())).add(LootItem.lootTableItem(block))));
        add(ModBlocks.PALE_MOSS_CARPET.get(), block -> LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1)).add(LootItem.lootTableItem(block).when(LootItemBlockStatePropertyCondition
                        .hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties()
                                .hasProperty(PaleMossCarpetBlock.BOTTOM, true)))))));
        add(ModBlocks.RESIN_CLUMP.get(), block -> createMultifaceBlockDrops(block,
                net.minecraft.world.level.storage.loot.predicates.ExplosionCondition.survivesExplosion()));
        add(ModBlocks.WILDFLOWERS.get(), this::createPetalsDrops);
        add(ModBlocks.LEAF_LITTER.get(), this::createSegmentDrops);
        add(ModBlocks.SHELF_MUSHROOM.get(), block -> LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(applyExplosionDecay(block, LootItem.lootTableItem(block)
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2)).when(LootItemBlockStatePropertyCondition
                                .hasBlockStateProperties(block).setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(ShelfMushroomBlock.AGE, 1))))))));
    }

    private LootTable.Builder createSegmentDrops(Block block) {
        return LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                .add(applyExplosionDecay(block, LootItem.lootTableItem(block).apply(IntStream.rangeClosed(1, 4).boxed().toList(),
                        amount -> SetItemCountFunction.setCount(ConstantValue.exactly(amount))
                                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                        .setProperties(StatePropertiesPredicate.Builder.properties()
                                                .hasProperty(LeafLitterBlock.SEGMENT_AMOUNT, amount)))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(h -> (Block) h.get()).toList();
    }
}
