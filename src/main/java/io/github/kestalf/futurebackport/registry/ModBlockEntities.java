package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.block.CopperFamily;
import io.github.kestalf.futurebackport.block.entity.CopperChestBlockEntity;
import io.github.kestalf.futurebackport.block.entity.CopperGolemStatueBlockEntity;
import io.github.kestalf.futurebackport.block.entity.CreakingHeartBlockEntity;
import io.github.kestalf.futurebackport.block.entity.ShelfBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("DataFlowIssue")
public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FutureBackport.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CreakingHeartBlockEntity>> CREAKING_HEART =
            BLOCK_ENTITIES.register("creaking_heart", () -> BlockEntityType.Builder.of(CreakingHeartBlockEntity::new,
                    ModBlocks.CREAKING_HEART.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperChestBlockEntity>> COPPER_CHEST =
            BLOCK_ENTITIES.register("copper_chest", () -> BlockEntityType.Builder.of(CopperChestBlockEntity::new,
                    blocks(ModBlocks.COPPER.chests)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CopperGolemStatueBlockEntity>> COPPER_GOLEM_STATUE =
            BLOCK_ENTITIES.register("copper_golem_statue", () -> BlockEntityType.Builder.of(CopperGolemStatueBlockEntity::new,
                    blocks(ModBlocks.COPPER.statues)).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShelfBlockEntity>> SHELF =
            BLOCK_ENTITIES.register("shelf", () -> BlockEntityType.Builder.of(ShelfBlockEntity::new,
                    ModBlocks.SHELVES.values().stream().map(holder -> (Block) holder.get()).toArray(Block[]::new)).build(null));

    private static Block[] blocks(CopperFamily.Weathering<?> weathering) {
        return weathering.all().stream().map(holder -> (Block) holder.get()).toArray(Block[]::new);
    }

    private ModBlockEntities() {
    }
}
