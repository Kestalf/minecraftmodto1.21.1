package io.github.kestalf.futurebackport.block;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModItems;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CeilingHangingSignBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallHangingSignBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** All blocks and items of one wood type (pale oak, poplar), mirroring the vanilla block family. */
public final class WoodFamily {
    public final String name;
    public final BlockSetType setType;
    public final WoodType woodType;

    public final DeferredBlock<RotatedPillarBlock> log;
    public final DeferredBlock<RotatedPillarBlock> wood;
    public final DeferredBlock<RotatedPillarBlock> strippedLog;
    public final DeferredBlock<RotatedPillarBlock> strippedWood;
    public final DeferredBlock<Block> planks;
    public final DeferredBlock<StairBlock> stairs;
    public final DeferredBlock<SlabBlock> slab;
    public final DeferredBlock<FenceBlock> fence;
    public final DeferredBlock<FenceGateBlock> fenceGate;
    public final DeferredBlock<DoorBlock> door;
    public final DeferredBlock<TrapDoorBlock> trapdoor;
    public final DeferredBlock<ButtonBlock> button;
    public final DeferredBlock<PressurePlateBlock> pressurePlate;
    public final DeferredBlock<StandingSignBlock> sign;
    public final DeferredBlock<WallSignBlock> wallSign;
    public final DeferredBlock<CeilingHangingSignBlock> hangingSign;
    public final DeferredBlock<WallHangingSignBlock> wallHangingSign;
    public final DeferredItem<SignItem> signItem;
    public final DeferredItem<HangingSignItem> hangingSignItem;
    public final DeferredBlock<SaplingBlock> sapling;
    public final DeferredBlock<FlowerPotBlock> pottedSapling;
    public final List<DeferredBlock<LeavesBlock>> leaves = new ArrayList<>();
    public final DeferredItem<BoatItem> boat;
    public final DeferredItem<BoatItem> chestBoat;

    private WoodFamily(Builder b) {
        this.name = b.name;
        String id = FutureBackport.MOD_ID + ":" + name;
        this.setType = BlockSetType.register(new BlockSetType(id));
        this.woodType = WoodType.register(new WoodType(id, setType));

        BlockBehaviour.Properties logProps = BlockBehaviour.Properties.of()
                .mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == net.minecraft.core.Direction.Axis.Y ? b.planksColor : b.barkColor)
                .instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.WOOD).ignitedByLava();
        this.log = ModBlocks.block(name + "_log", () -> new RotatedPillarBlock(logProps));
        this.wood = ModBlocks.block(name + "_wood", () -> new RotatedPillarBlock(props(b.barkColor).strength(2.0F)));
        this.strippedLog = ModBlocks.block("stripped_" + name + "_log", () -> new RotatedPillarBlock(props(b.planksColor).strength(2.0F)));
        this.strippedWood = ModBlocks.block("stripped_" + name + "_wood", () -> new RotatedPillarBlock(props(b.planksColor).strength(2.0F)));
        this.planks = ModBlocks.block(name + "_planks", () -> new Block(props(b.planksColor).strength(2.0F, 3.0F)));
        this.stairs = ModBlocks.block(name + "_stairs", () -> new StairBlock(planks.get().defaultBlockState(), BlockBehaviour.Properties.ofFullCopy(planks.get())));
        this.slab = ModBlocks.block(name + "_slab", () -> new SlabBlock(BlockBehaviour.Properties.ofFullCopy(planks.get())));
        this.fence = ModBlocks.block(name + "_fence", () -> new FenceBlock(props(b.planksColor).forceSolidOn().strength(2.0F, 3.0F)));
        this.fenceGate = ModBlocks.block(name + "_fence_gate", () -> new FenceGateBlock(woodType, props(b.planksColor).forceSolidOn().strength(2.0F, 3.0F)));
        this.door = ModBlocks.block(name + "_door", () -> new DoorBlock(setType,
                props(b.planksColor).strength(3.0F).noOcclusion().pushReaction(PushReaction.DESTROY)));
        this.trapdoor = ModBlocks.block(name + "_trapdoor", () -> new TrapDoorBlock(setType,
                props(b.planksColor).strength(3.0F).noOcclusion().isValidSpawn(Blocks::never)));
        this.button = ModBlocks.block(name + "_button", () -> new ButtonBlock(setType, 30,
                BlockBehaviour.Properties.of().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));
        this.pressurePlate = ModBlocks.block(name + "_pressure_plate", () -> new PressurePlateBlock(setType,
                props(b.planksColor).forceSolidOn().noCollission().strength(0.5F).pushReaction(PushReaction.DESTROY)));

        this.sign = ModBlocks.blockNoItem(name + "_sign", () -> new StandingSignBlock(woodType,
                props(b.planksColor).forceSolidOn().noCollission().strength(1.0F)));
        this.wallSign = ModBlocks.blockNoItem(name + "_wall_sign", () -> new WallSignBlock(woodType,
                props(b.planksColor).forceSolidOn().noCollission().strength(1.0F).lootFrom(sign)));
        this.hangingSign = ModBlocks.blockNoItem(name + "_hanging_sign", () -> new CeilingHangingSignBlock(woodType,
                props(b.barkColor).forceSolidOn().noCollission().strength(1.0F)));
        this.wallHangingSign = ModBlocks.blockNoItem(name + "_wall_hanging_sign", () -> new WallHangingSignBlock(woodType,
                props(b.barkColor).forceSolidOn().noCollission().strength(1.0F).lootFrom(hangingSign)));
        this.signItem = ModItems.ITEMS.register(name + "_sign",
                () -> new SignItem(new Item.Properties().stacksTo(16), sign.get(), wallSign.get()));
        this.hangingSignItem = ModItems.ITEMS.register(name + "_hanging_sign",
                () -> new HangingSignItem(hangingSign.get(), wallHangingSign.get(), new Item.Properties().stacksTo(16)));

        for (LeavesSpec spec : b.leaves) {
            leaves.add(ModBlocks.block(spec.name, () -> new FallingParticleLeavesBlock(spec.particle,
                    BlockBehaviour.Properties.of().mapColor(spec.color).strength(0.2F).randomTicks().sound(SoundType.GRASS)
                            .noOcclusion().isValidSpawn(Blocks::ocelotOrParrot).isSuffocating((s, l, p) -> false)
                            .isViewBlocking((s, l, p) -> false).ignitedByLava().pushReaction(PushReaction.DESTROY)
                            .isRedstoneConductor((s, l, p) -> false))));
        }
        TreeGrower grower = b.grower.get();
        this.sapling = ModBlocks.block(name + "_sapling", () -> new SaplingBlock(grower,
                BlockBehaviour.Properties.of().mapColor(b.leaves.getFirst().color).noCollission().randomTicks().instabreak()
                        .sound(SoundType.GRASS).pushReaction(PushReaction.DESTROY)));
        this.pottedSapling = ModBlocks.blockNoItem("potted_" + name + "_sapling", () -> new FlowerPotBlock(
                () -> (FlowerPotBlock) Blocks.FLOWER_POT, sapling, BlockBehaviour.Properties.of().instabreak().noOcclusion()
                .pushReaction(PushReaction.DESTROY)));

        this.boat = ModItems.ITEMS.register(name + "_boat", () -> new BoatItem(false, b.boatType.get(), new Item.Properties().stacksTo(1)));
        this.chestBoat = ModItems.ITEMS.register(name + "_chest_boat", () -> new BoatItem(true, b.boatType.get(), new Item.Properties().stacksTo(1)));
    }

    private static BlockBehaviour.Properties props(MapColor color) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS).sound(SoundType.WOOD).ignitedByLava();
    }

    /** Blocks that burn like vanilla wood: (block, encouragement, flammability). */
    public void forEachFlammable(FlammableConsumer consumer) {
        for (var b : List.of(planks, stairs, slab, fence, fenceGate)) consumer.accept(b.get(), 5, 20);
        for (var b : List.of(log, wood, strippedLog, strippedWood)) consumer.accept(b.get(), 5, 5);
        for (var b : leaves) consumer.accept(b.get(), 30, 60);
    }

    @FunctionalInterface
    public interface FlammableConsumer {
        void accept(Block block, int encouragement, int flammability);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    private record LeavesSpec(String name, MapColor color, Supplier<? extends net.minecraft.core.particles.ParticleOptions> particle) {
    }

    public static final class Builder {
        private final String name;
        private MapColor planksColor = MapColor.WOOD;
        private MapColor barkColor = MapColor.PODZOL;
        private final List<LeavesSpec> leaves = new ArrayList<>();
        private Supplier<TreeGrower> grower;
        private Supplier<Boat.Type> boatType;

        private Builder(String name) {
            this.name = name;
        }

        public Builder colors(MapColor planks, MapColor bark) {
            this.planksColor = planks;
            this.barkColor = bark;
            return this;
        }

        public Builder leaves(String name, MapColor color, Supplier<? extends net.minecraft.core.particles.ParticleOptions> particle) {
            leaves.add(new LeavesSpec(name, color, particle));
            return this;
        }

        public Builder grower(Supplier<TreeGrower> grower) {
            this.grower = grower;
            return this;
        }

        public Builder boat(Supplier<Boat.Type> boatType) {
            this.boatType = boatType;
            return this;
        }

        public WoodFamily build() {
            return new WoodFamily(this);
        }
    }
}
