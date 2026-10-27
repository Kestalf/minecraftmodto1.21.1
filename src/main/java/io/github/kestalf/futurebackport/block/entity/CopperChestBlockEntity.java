package io.github.kestalf.futurebackport.block.entity;

import io.github.kestalf.futurebackport.block.CopperChestBlock;
import io.github.kestalf.futurebackport.registry.ModBlockEntities;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** A chest block entity with the copper chest sounds (which depend on the oxidation stage). */
public class CopperChestBlockEntity extends ChestBlockEntity {
    public CopperChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPER_CHEST.get(), pos, state);
        this.openersCounter = new ContainerOpenersCounter() {
            @Override
            protected void onOpen(Level level, BlockPos pos, BlockState state) {
                playSound(level, pos, state, true);
            }

            @Override
            protected void onClose(Level level, BlockPos pos, BlockState state) {
                playSound(level, pos, state, false);
            }

            @Override
            protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int count, int openCount) {
                signalOpenCount(level, pos, state, count, openCount);
            }

            @Override
            protected boolean isOwnContainer(Player player) {
                if (!(player.containerMenu instanceof ChestMenu menu)) return false;
                var container = menu.getContainer();
                return container == CopperChestBlockEntity.this
                        || container instanceof CompoundContainer compound && compound.contains(CopperChestBlockEntity.this);
            }
        };
    }

    private static void playSound(Level level, BlockPos pos, BlockState state, boolean open) {
        ChestType type = state.getValue(ChestBlock.TYPE);
        if (type == ChestType.LEFT) return;
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (type == ChestType.RIGHT) {
            Direction direction = ChestBlock.getConnectedDirection(state);
            x += direction.getStepX() * 0.5;
            z += direction.getStepZ() * 0.5;
        }
        level.playSound(null, x, y, z, sound(state, open), SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1F + 0.9F);
    }

    private static SoundEvent sound(BlockState state, boolean open) {
        WeatherState age = state.getBlock() instanceof CopperChestBlock chest ? chest.getAge() : WeatherState.UNAFFECTED;
        String base = switch (age) {
            case UNAFFECTED, EXPOSED -> "block.copper_chest";
            case WEATHERED -> "block.copper_chest_weathered";
            case OXIDIZED -> "block.copper_chest_oxidized";
        };
        return ModSounds.event(base + (open ? ".open" : ".close")).get();
    }
}
