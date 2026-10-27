package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.block.CopperFamily;
import io.github.kestalf.futurebackport.block.StoneFamily;
import io.github.kestalf.futurebackport.block.WoodFamily;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import io.github.kestalf.futurebackport.block.plant.CactusFlowerBlock;
import io.github.kestalf.futurebackport.block.plant.DryVegetationBlock;
import io.github.kestalf.futurebackport.block.plant.EyeblossomBlock;
import io.github.kestalf.futurebackport.block.plant.FireflyBushBlock;
import io.github.kestalf.futurebackport.block.plant.HangingMossBlock;
import io.github.kestalf.futurebackport.block.plant.LeafLitterBlock;
import io.github.kestalf.futurebackport.block.plant.PaleMossBlock;
import io.github.kestalf.futurebackport.block.plant.PaleMossCarpetBlock;
import io.github.kestalf.futurebackport.block.plant.ResinClumpBlock;
import io.github.kestalf.futurebackport.block.plant.ShelfMushroomBlock;
import io.github.kestalf.futurebackport.block.plant.SpreadingBushBlock;
import io.github.kestalf.futurebackport.item.GoldenDandelionItem;
import io.github.kestalf.futurebackport.worldgen.ModFeatureKeys;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.PinkPetalsBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.Optional;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FutureBackport.MOD_ID);

    /** Every block that gets a block item, in registration order (used for the creative tab and data generation). */
    public static final List<DeferredBlock<? extends Block>> WITH_ITEMS = new ArrayList<>();

    // ------------------------------------------------------------------ wood

    public static final WoodFamily PALE_OAK = WoodFamily.builder("pale_oak")
            .colors(MapColor.QUARTZ, MapColor.STONE)
            .leaves("pale_oak_leaves", MapColor.METAL, () -> ModParticles.PALE_OAK_LEAVES)
            .grower(() -> new TreeGrower(FutureBackport.MOD_ID + ":pale_oak", Optional.of(ModFeatureKeys.PALE_OAK_BONEMEAL),
                    Optional.empty(), Optional.empty()))
            .boat(ModBoatTypes.PALE_OAK::getValue)
            .build();
    public static final WoodFamily POPLAR = WoodFamily.builder("poplar")
            .colors(MapColor.TERRACOTTA_WHITE, MapColor.TERRACOTTA_GRAY)
            .leaves("red_poplar_leaves", MapColor.COLOR_RED, () -> ModParticles.RED_POPLAR_LEAVES)
            .leaves("orange_poplar_leaves", MapColor.COLOR_ORANGE, () -> ModParticles.ORANGE_POPLAR_LEAVES)
            .leaves("yellow_poplar_leaves", MapColor.COLOR_YELLOW, () -> ModParticles.YELLOW_POPLAR_LEAVES)
            .grower(() -> new TreeGrower(FutureBackport.MOD_ID + ":poplar", Optional.empty(), Optional.of(ModFeatureKeys.POPLAR),
                    Optional.empty()))
            .boat(ModBoatTypes.POPLAR::getValue)
            .build();

    // ------------------------------------------------------------------ plants

    public static final DeferredBlock<PaleMossBlock> PALE_MOSS_BLOCK = block("pale_moss_block", () -> new PaleMossBlock(
            BlockBehaviour.Properties.of().ignitedByLava().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.1F).sound(SoundType.MOSS)
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<PaleMossCarpetBlock> PALE_MOSS_CARPET = block("pale_moss_carpet", () -> new PaleMossCarpetBlock(
            BlockBehaviour.Properties.of().ignitedByLava().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.1F).sound(SoundType.MOSS_CARPET)
                    .noOcclusion().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<HangingMossBlock> PALE_HANGING_MOSS = block("pale_hanging_moss", () -> new HangingMossBlock(
            BlockBehaviour.Properties.of().ignitedByLava().mapColor(MapColor.COLOR_LIGHT_GRAY).noCollission().instabreak()
                    .sound(SoundType.MOSS_CARPET).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<EyeblossomBlock> OPEN_EYEBLOSSOM = block("open_eyeblossom", () -> new EyeblossomBlock(true, flower()));
    public static final DeferredBlock<EyeblossomBlock> CLOSED_EYEBLOSSOM = block("closed_eyeblossom", () -> new EyeblossomBlock(false, flower()));
    public static final DeferredBlock<FlowerPotBlock> POTTED_OPEN_EYEBLOSSOM = potted("open_eyeblossom", OPEN_EYEBLOSSOM);
    public static final DeferredBlock<FlowerPotBlock> POTTED_CLOSED_EYEBLOSSOM = potted("closed_eyeblossom", CLOSED_EYEBLOSSOM);
    public static final DeferredBlock<ResinClumpBlock> RESIN_CLUMP = block("resin_clump", () -> new ResinClumpBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).replaceable().noCollission().sound(ModSounds.RESIN_TYPE)
                    .ignitedByLava().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<Block> RESIN_BLOCK = block("resin_block", () -> new Block(
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).instrument(NoteBlockInstrument.BASEDRUM)
                    .sound(ModSounds.RESIN_TYPE)));

    public static final DeferredBlock<LeafLitterBlock> LEAF_LITTER = block("leaf_litter", () -> new LeafLitterBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).replaceable().noCollission().sound(ModSounds.LEAF_LITTER_TYPE)
                    .ignitedByLava().pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<PinkPetalsBlock> WILDFLOWERS = block("wildflowers", () -> new PinkPetalsBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().sound(SoundType.PINK_PETALS)
                    .pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<SpreadingBushBlock> BUSH = block("bush", () -> new SpreadingBushBlock(plant(SoundType.GRASS)));
    public static final DeferredBlock<FireflyBushBlock> FIREFLY_BUSH = block("firefly_bush", () -> new FireflyBushBlock(
            plant(SoundType.SWEET_BERRY_BUSH).lightLevel(state -> 2)));
    public static final DeferredBlock<DryVegetationBlock> SHORT_DRY_GRASS = block("short_dry_grass", () -> new DryVegetationBlock(
            Block.box(2, 0, 2, 14, 10, 14), () -> ModBlocks.TALL_DRY_GRASS.get(), null, plant(SoundType.GRASS).mapColor(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<DryVegetationBlock> TALL_DRY_GRASS = block("tall_dry_grass", () -> new DryVegetationBlock(
            Block.box(1, 0, 1, 15, 16, 15), null, () -> ModBlocks.SHORT_DRY_GRASS.get(), plant(SoundType.GRASS).mapColor(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<CactusFlowerBlock> CACTUS_FLOWER = block("cactus_flower", () -> new CactusFlowerBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollission().instabreak().ignitedByLava()
                    .sound(ModSounds.CACTUS_FLOWER_TYPE).pushReaction(PushReaction.DESTROY)));
    public static final DeferredBlock<FlowerBlock> GOLDEN_DANDELION = blockWithItem("golden_dandelion",
            () -> new FlowerBlock(MobEffects.SATURATION, 0.35F, flower()),
            block -> new GoldenDandelionItem(block.get(), new Item.Properties()));
    public static final DeferredBlock<FlowerPotBlock> POTTED_GOLDEN_DANDELION = potted("golden_dandelion", GOLDEN_DANDELION);
    public static final DeferredBlock<SpreadingBushBlock> RED_SHRUB = block("red_shrub", () -> new SpreadingBushBlock(
            plant(ModSounds.RED_SHRUB_TYPE).mapColor(MapColor.COLOR_RED)));
    public static final DeferredBlock<ShelfMushroomBlock> SHELF_MUSHROOM = block("shelf_mushroom", () -> new ShelfMushroomBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.2F).noOcclusion().ignitedByLava()
                    .sound(ModSounds.SHELF_MUSHROOM_TYPE).pushReaction(PushReaction.DESTROY)));

    // ------------------------------------------------------------------ stone-like families

    public static final StoneFamily RESIN_BRICKS = StoneFamily.of("resin_bricks", "resin_brick",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
                    .sound(ModSounds.RESIN_BRICKS_TYPE));
    public static final DeferredBlock<Block> CHISELED_RESIN_BRICKS = block("chiseled_resin_bricks",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(RESIN_BRICKS.base().get())));

    private static BlockBehaviour.Properties cinnabar() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
                .sound(ModSounds.CINNABAR_TYPE);
    }

    private static BlockBehaviour.Properties sulfur() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).requiresCorrectToolForDrops().strength(1.5F, 6.0F)
                .sound(ModSounds.SULFUR_TYPE);
    }

    public static final StoneFamily CINNABAR = StoneFamily.of("cinnabar", "cinnabar", cinnabar());
    public static final StoneFamily POLISHED_CINNABAR = StoneFamily.of("polished_cinnabar", "polished_cinnabar", cinnabar());
    public static final StoneFamily CINNABAR_BRICKS = StoneFamily.of("cinnabar_bricks", "cinnabar_brick", cinnabar());
    public static final DeferredBlock<Block> CHISELED_CINNABAR = block("chiseled_cinnabar", () -> new Block(cinnabar()));

    public static final StoneFamily SULFUR = StoneFamily.of("sulfur", "sulfur", sulfur());
    public static final StoneFamily POLISHED_SULFUR = StoneFamily.of("polished_sulfur", "polished_sulfur", sulfur());
    public static final StoneFamily SULFUR_BRICKS = StoneFamily.of("sulfur_bricks", "sulfur_brick", sulfur());
    public static final DeferredBlock<Block> CHISELED_SULFUR = block("chiseled_sulfur", () -> new Block(sulfur()));

    // ------------------------------------------------------------------ wool / concrete stairs and slabs

    public static final Map<DyeColor, DeferredBlock<StairBlock>> WOOL_STAIRS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredBlock<SlabBlock>> WOOL_SLABS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredBlock<StairBlock>> CONCRETE_STAIRS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredBlock<SlabBlock>> CONCRETE_SLABS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor color : DyeColor.values()) {
            Block wool = VanillaColored.wool(color);
            WOOL_STAIRS.put(color, block(color.getName() + "_wool_stairs",
                    () -> new StairBlock(wool.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(wool))));
            WOOL_SLABS.put(color, block(color.getName() + "_wool_slab",
                    () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(wool))));
        }
        for (DyeColor color : DyeColor.values()) {
            Block concrete = VanillaColored.concrete(color);
            CONCRETE_STAIRS.put(color, block(color.getName() + "_concrete_stairs",
                    () -> new StairBlock(concrete.defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(concrete))));
            CONCRETE_SLABS.put(color, block(color.getName() + "_concrete_slab",
                    () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(concrete))));
        }
    }

    // ------------------------------------------------------------------ copper

    public static final CopperFamily COPPER = CopperFamily.create();

    // ------------------------------------------------------------------ registration helpers

    private static BlockBehaviour.Properties flower() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY);
    }

    private static BlockBehaviour.Properties plant(SoundType sound) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).replaceable().noCollission().instabreak().sound(sound)
                .offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.DESTROY);
    }

    private static DeferredBlock<FlowerPotBlock> potted(String name, Supplier<? extends Block> plant) {
        return blockNoItem("potted_" + name, () -> new FlowerPotBlock(() -> (FlowerPotBlock) Blocks.FLOWER_POT, plant,
                BlockBehaviour.Properties.of().instabreak().noOcclusion().pushReaction(PushReaction.DESTROY)));
    }

    public static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> factory) {
        return block(name, factory, new Item.Properties());
    }

    public static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> factory, Item.Properties itemProperties) {
        DeferredBlock<T> block = BLOCKS.register(name, factory);
        ModItems.ITEMS.registerSimpleBlockItem(block, itemProperties);
        WITH_ITEMS.add(block);
        return block;
    }

    public static <T extends Block> DeferredBlock<T> blockWithItem(String name, Supplier<T> factory,
                                                                   Function<DeferredBlock<T>, ? extends Item> item) {
        DeferredBlock<T> block = BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> item.apply(block));
        WITH_ITEMS.add(block);
        return block;
    }

    public static <T extends Block> DeferredBlock<T> blockNoItem(String name, Supplier<T> factory) {
        return BLOCKS.register(name, factory);
    }

    private ModBlocks() {
    }

    /** Vanilla wool / concrete lookups by dye color. */
    public static final class VanillaColored {
        public static Block wool(DyeColor color) {
            return switch (color) {
                case WHITE -> Blocks.WHITE_WOOL;
                case ORANGE -> Blocks.ORANGE_WOOL;
                case MAGENTA -> Blocks.MAGENTA_WOOL;
                case LIGHT_BLUE -> Blocks.LIGHT_BLUE_WOOL;
                case YELLOW -> Blocks.YELLOW_WOOL;
                case LIME -> Blocks.LIME_WOOL;
                case PINK -> Blocks.PINK_WOOL;
                case GRAY -> Blocks.GRAY_WOOL;
                case LIGHT_GRAY -> Blocks.LIGHT_GRAY_WOOL;
                case CYAN -> Blocks.CYAN_WOOL;
                case PURPLE -> Blocks.PURPLE_WOOL;
                case BLUE -> Blocks.BLUE_WOOL;
                case BROWN -> Blocks.BROWN_WOOL;
                case GREEN -> Blocks.GREEN_WOOL;
                case RED -> Blocks.RED_WOOL;
                case BLACK -> Blocks.BLACK_WOOL;
            };
        }

        public static Block concrete(DyeColor color) {
            return switch (color) {
                case WHITE -> Blocks.WHITE_CONCRETE;
                case ORANGE -> Blocks.ORANGE_CONCRETE;
                case MAGENTA -> Blocks.MAGENTA_CONCRETE;
                case LIGHT_BLUE -> Blocks.LIGHT_BLUE_CONCRETE;
                case YELLOW -> Blocks.YELLOW_CONCRETE;
                case LIME -> Blocks.LIME_CONCRETE;
                case PINK -> Blocks.PINK_CONCRETE;
                case GRAY -> Blocks.GRAY_CONCRETE;
                case LIGHT_GRAY -> Blocks.LIGHT_GRAY_CONCRETE;
                case CYAN -> Blocks.CYAN_CONCRETE;
                case PURPLE -> Blocks.PURPLE_CONCRETE;
                case BLUE -> Blocks.BLUE_CONCRETE;
                case BROWN -> Blocks.BROWN_CONCRETE;
                case GREEN -> Blocks.GREEN_CONCRETE;
                case RED -> Blocks.RED_CONCRETE;
                case BLACK -> Blocks.BLACK_CONCRETE;
            };
        }

        private VanillaColored() {
        }
    }
}
