package io.github.kestalf.futurebackport.block;

import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModItems;
import io.github.kestalf.futurebackport.registry.ModParticles;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The copper blocks of "The Copper Age": bars, chains, lanterns, torches and the oxidizing lightning rods.
 * Every oxidizing block has an unwaxed and a waxed version for each {@link WeatherState}.
 */
public final class CopperFamily {
    public final Weathering<IronBarsBlock> bars;
    public final Weathering<ChainBlock> chains;
    public final Weathering<LanternBlock> lanterns;
    /** The unaffected unwaxed lightning rod is the vanilla one, so {@code lightningRods.get(UNAFFECTED)} is empty. */
    public final Weathering<LightningRodBlock> lightningRods;
    /** Copper chests and golem statues have block entities and their own loot, so {@link #all()} leaves them out. */
    public final Weathering<CopperChestBlock> chests;
    public final Weathering<CopperGolemStatueBlock> statues;
    public final DeferredBlock<TorchBlock> torch;
    public final DeferredBlock<WallTorchBlock> wallTorch;
    public final DeferredItem<StandingAndWallBlockItem> torchItem;

    private CopperFamily() {
        bars = Weathering.create("copper_bars", true,
                (state, props) -> new WeatheringCopperBarsBlock(state, props), IronBarsBlock::new,
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).sound(net.minecraft.world.level.block.SoundType.COPPER));
        chains = Weathering.create("copper_chain", true,
                (state, props) -> new WeatheringCopperChainBlock(state, props), ChainBlock::new,
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN).sound(net.minecraft.world.level.block.SoundType.CHAIN));
        lanterns = Weathering.create("copper_lantern", true,
                (state, props) -> new WeatheringCopperLanternBlock(state, props), LanternBlock::new,
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN));
        lightningRods = Weathering.create("lightning_rod", false,
                (state, props) -> new WeatheringLightningRodBlock(state, props), LightningRodBlock::new,
                () -> BlockBehaviour.Properties.ofFullCopy(Blocks.LIGHTNING_ROD));

        chests = Weathering.create("copper_chest", true,
                (state, props) -> new CopperChestBlock(state, false, props), (state, props) -> new CopperChestBlock(state, true, props),
                state -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHEST).mapColor(mapColor(state)).strength(3.0F, 6.0F)
                        .sound(net.minecraft.world.level.block.SoundType.COPPER).requiresCorrectToolForDrops());
        statues = Weathering.create("copper_golem_statue", true,
                (state, props) -> new CopperGolemStatueBlock(state, false, props), (state, props) -> new CopperGolemStatueBlock(state, true, props),
                state -> BlockBehaviour.Properties.of().mapColor(mapColor(state)).strength(3.0F, 6.0F).noOcclusion()
                        .sound(ModSounds.COPPER_GOLEM_STATUE_TYPE).pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));

        torch = ModBlocks.blockNoItem("copper_torch", () -> new TorchBlock(ModParticles.COPPER_FIRE_FLAME,
                BlockBehaviour.Properties.ofFullCopy(Blocks.TORCH)));
        wallTorch = ModBlocks.blockNoItem("copper_wall_torch", () -> new WallTorchBlock(ModParticles.COPPER_FIRE_FLAME,
                BlockBehaviour.Properties.ofFullCopy(Blocks.WALL_TORCH).lootFrom(torch)));
        torchItem = ModItems.ITEMS.register("copper_torch",
                () -> new StandingAndWallBlockItem(torch.get(), wallTorch.get(), new Item.Properties(), Direction.DOWN));
    }

    public static CopperFamily create() {
        return new CopperFamily();
    }

    public static MapColor mapColor(WeatherState state) {
        return switch (state) {
            case UNAFFECTED -> MapColor.COLOR_ORANGE;
            case EXPOSED -> MapColor.TERRACOTTA_LIGHT_GRAY;
            case WEATHERED -> MapColor.WARPED_STEM;
            case OXIDIZED -> MapColor.WARPED_NYLIUM;
        };
    }

    /** All registered blocks of this family. */
    public List<DeferredBlock<? extends Block>> all() {
        List<DeferredBlock<? extends Block>> list = new ArrayList<>();
        for (Weathering<?> w : List.of(bars, chains, lanterns, lightningRods)) list.addAll(w.all());
        list.add(torch);
        list.add(wallTorch);
        return list;
    }

    /**
     * One oxidizing block type: an unwaxed and a waxed block for each oxidation stage. Names follow vanilla:
     * {@code copper_bars}, {@code exposed_copper_bars}, {@code waxed_copper_bars}, {@code waxed_exposed_copper_bars}...
     */
    public static final class Weathering<B extends Block> {
        public final Map<WeatherState, DeferredBlock<B>> unwaxed = new EnumMap<>(WeatherState.class);
        public final Map<WeatherState, DeferredBlock<B>> waxed = new EnumMap<>(WeatherState.class);

        static <B extends Block> Weathering<B> create(String baseName, boolean includeUnaffected,
                                                     BiFunction<WeatherState, BlockBehaviour.Properties, ? extends B> weathering,
                                                     Function<BlockBehaviour.Properties, ? extends B> plain,
                                                     Supplier<BlockBehaviour.Properties> properties) {
            return create(baseName, includeUnaffected, weathering, (state, props) -> plain.apply(props), state -> properties.get());
        }

        /** Variant where the waxed blocks also know their oxidation stage (e.g. for textures) and properties depend on it. */
        public static <B extends Block> Weathering<B> create(String baseName, boolean includeUnaffected,
                                                            BiFunction<WeatherState, BlockBehaviour.Properties, ? extends B> weathering,
                                                            BiFunction<WeatherState, BlockBehaviour.Properties, ? extends B> waxed,
                                                            Function<WeatherState, BlockBehaviour.Properties> properties) {
            Weathering<B> result = new Weathering<>();
            for (WeatherState state : WeatherState.values()) {
                String name = prefix(state) + baseName;
                if (state != WeatherState.UNAFFECTED || includeUnaffected) {
                    result.unwaxed.put(state, ModBlocks.block(name, () -> weathering.apply(state, properties.apply(state))));
                }
            }
            for (WeatherState state : WeatherState.values()) {
                result.waxed.put(state, ModBlocks.block("waxed_" + prefix(state) + baseName, () -> waxed.apply(state, properties.apply(state))));
            }
            return result;
        }

        /** The block for a stage, unwaxed or waxed. */
        public B get(WeatherState state, boolean isWaxed) {
            return (isWaxed ? waxed : unwaxed).get(state).get();
        }

        public List<DeferredBlock<B>> all() {
            List<DeferredBlock<B>> list = new ArrayList<>(unwaxed.values());
            list.addAll(waxed.values());
            return list;
        }

        public static String prefix(WeatherState state) {
            return state == WeatherState.UNAFFECTED ? "" : state.getSerializedName() + "_";
        }
    }
}
