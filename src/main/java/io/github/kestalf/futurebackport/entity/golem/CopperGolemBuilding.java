package io.github.kestalf.futurebackport.entity.golem;

import io.github.kestalf.futurebackport.block.CopperChestBlock;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Building a copper golem: a carved pumpkin (or jack o'lantern) placed last next to a block of copper of any stage
 * turns the copper into a copper chest and spawns a golem of the same stage. Waxed copper works but gives unwaxed
 * results.
 */
public final class CopperGolemBuilding {
    private static final Map<Block, WeatherState> COPPER_BLOCKS = Map.of(
            Blocks.COPPER_BLOCK, WeatherState.UNAFFECTED, Blocks.EXPOSED_COPPER, WeatherState.EXPOSED,
            Blocks.WEATHERED_COPPER, WeatherState.WEATHERED, Blocks.OXIDIZED_COPPER, WeatherState.OXIDIZED,
            Blocks.WAXED_COPPER_BLOCK, WeatherState.UNAFFECTED, Blocks.WAXED_EXPOSED_COPPER, WeatherState.EXPOSED,
            Blocks.WAXED_WEATHERED_COPPER, WeatherState.WEATHERED, Blocks.WAXED_OXIDIZED_COPPER, WeatherState.OXIDIZED);
    /** The copper block below the pumpkin is checked first, then the sides and the one above. */
    private static final Direction[] SEARCH_ORDER = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};

    @Nullable
    public static BlockPos findCopper(LevelReader level, BlockPos pumpkin) {
        for (Direction direction : SEARCH_ORDER) {
            BlockPos pos = pumpkin.relative(direction);
            if (COPPER_BLOCKS.containsKey(level.getBlockState(pos).getBlock())) return pos;
        }
        return null;
    }

    public static boolean trySpawn(Level level, BlockPos pumpkinPos) {
        if (!(level instanceof ServerLevel server)) return false;
        BlockState pumpkin = level.getBlockState(pumpkinPos);
        if (!(pumpkin.getBlock() instanceof CarvedPumpkinBlock)) return false;
        BlockPos copperPos = findCopper(level, pumpkinPos);
        if (copperPos == null) return false;
        WeatherState stage = COPPER_BLOCKS.get(level.getBlockState(copperPos).getBlock());
        Direction facing = pumpkin.getValue(CarvedPumpkinBlock.FACING);

        level.setBlock(pumpkinPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.levelEvent(2001, pumpkinPos, Block.getId(pumpkin));
        CopperChestBlock chest = ModBlocks.COPPER.chests.get(stage, false);
        BlockState chestState = chest.defaultBlockState().setValue(ChestBlock.FACING, facing).setValue(ChestBlock.TYPE, ChestType.SINGLE);
        level.setBlock(copperPos, chestState, Block.UPDATE_ALL);

        CopperGolem golem = CopperGolem.spawnBuilt(server, Vec3.atBottomCenterOf(pumpkinPos), facing.toYRot(), stage);
        if (golem != null) {
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pumpkinPos).inflate(5.0))) {
                CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
            }
        }
        level.updateNeighborsAt(pumpkinPos, Blocks.AIR);
        return true;
    }

    private CopperGolemBuilding() {
    }
}
