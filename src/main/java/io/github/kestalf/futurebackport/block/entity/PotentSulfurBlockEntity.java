package io.github.kestalf.futurebackport.block.entity;

import io.github.kestalf.futurebackport.block.PotentSulfurBlock;
import io.github.kestalf.futurebackport.block.PotentSulfurBlock.State;
import io.github.kestalf.futurebackport.registry.ModBlockEntities;
import io.github.kestalf.futurebackport.registry.ModParticles;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Noxious gas and geysers of potent sulfur. Both need a column of 1 to 4 water source blocks above the sulfur with air
 * on top. The gas gives Nausea in the water within 3 blocks of the surface spot; a geyser launches everything in its
 * column upwards to about five times the water depth. Periodic geysers (over magma) stay dormant for
 * {@code 10 * (depth - 1) + 15..30} seconds and erupt for {@code depth - 1 + 1..2} seconds; the two random values
 * depend on the position.
 */
public class PotentSulfurBlockEntity extends BlockEntity {
    private static final int MAX_DEPTH = 4;
    private static final int GAS_RADIUS = 3;
    private int countdown = -1;

    public PotentSulfurBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POTENT_SULFUR.get(), pos, state);
    }

    /** Depth of the still water column above, or 0 if there is no valid column. */
    public static int waterDepth(Level level, BlockPos pos) {
        for (int depth = 1; depth <= MAX_DEPTH + 1; depth++) {
            FluidState fluid = level.getFluidState(pos.above(depth));
            if (fluid.is(FluidTags.WATER)) {
                if (!fluid.isSource()) return 0;
                continue;
            }
            int water = depth - 1;
            return water >= 1 && water <= MAX_DEPTH && level.getBlockState(pos.above(depth)).isAir() ? water : 0;
        }
        return 0;
    }

    private static int dormantSeconds(BlockPos pos, int depth) {
        return 10 * (depth - 1) + RandomSource.create(pos.asLong()).nextIntBetweenInclusive(15, 30);
    }

    private static int eruptionSeconds(BlockPos pos, int depth) {
        RandomSource random = RandomSource.create(pos.asLong());
        random.nextInt();
        return depth - 1 + random.nextIntBetweenInclusive(1, 2);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PotentSulfurBlockEntity sulfur) {
        State phase = state.getValue(PotentSulfurBlock.STATE);
        if (phase == State.DRY) return;
        int depth = waterDepth(level, pos);
        if (depth == 0) return;
        long time = level.getGameTime();
        if (time % 20 == 0) {
            poisonSwimmers(level, pos, depth);
            if (phase == State.DORMANT || phase == State.ERUPTING) {
                if (sulfur.countdown < 0) sulfur.countdown = phase == State.DORMANT ? dormantSeconds(pos, depth) : eruptionSeconds(pos, depth);
                if (--sulfur.countdown <= 0) {
                    State next = phase == State.DORMANT ? State.ERUPTING : State.DORMANT;
                    level.setBlock(pos, state.setValue(PotentSulfurBlock.STATE, next), Block.UPDATE_ALL);
                    sulfur.countdown = next == State.DORMANT ? dormantSeconds(pos, depth) : eruptionSeconds(pos, depth);
                    if (next == State.ERUPTING) {
                        level.playSound(null, pos.above(depth), ModSounds.event("block.potent_sulfur.geyser_eruption").get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                    phase = next;
                }
                sulfur.setChanged();
            }
            if (phase.isGeyser() && time % 40 == 0) {
                String sound = phase == State.CONTINUOUS ? "block.potent_sulfur.geyser_continuous_eruption_active" : "block.potent_sulfur.geyser_eruption_active";
                level.playSound(null, pos.above(depth), ModSounds.event(sound).get(), SoundSource.BLOCKS, 0.8F, 1.0F);
            } else if (!phase.isGeyser() && time % 80 == 0 && level.random.nextInt(3) == 0) {
                level.playSound(null, pos.above(depth), ModSounds.event("block.potent_sulfur.noxious_gas").get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            }
        }
        if (phase.isGeyser()) launch(level, pos, depth);
    }

    private static void poisonSwimmers(Level level, BlockPos pos, int depth) {
        BlockPos surface = pos.above(depth);
        AABB area = new AABB(surface).inflate(GAS_RADIUS, 0, GAS_RADIUS).expandTowards(0, 1, 0);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area, LivingEntity::isInWater)) {
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, true));
        }
    }

    /** Pushes everything in the column upwards fast enough to fly about five blocks per block of water. */
    private static void launch(Level level, BlockPos pos, int depth) {
        double speed = Math.sqrt(2.0 * 0.08 * 5.0 * depth) * 1.1;
        AABB column = new AABB(pos.above()).expandTowards(0, depth + 1, 0);
        for (Entity entity : level.getEntities((Entity) null, column, entity -> !entity.isSpectator())) {
            Vec3 motion = entity.getDeltaMovement();
            if (motion.y < speed) {
                entity.setDeltaMovement(motion.x, speed, motion.z);
                entity.resetFallDistance();
                entity.hurtMarked = true;
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, PotentSulfurBlockEntity sulfur) {
        State phase = state.getValue(PotentSulfurBlock.STATE);
        if (phase == State.DRY) return;
        int depth = waterDepth(level, pos);
        if (depth == 0) return;
        RandomSource random = level.random;
        double x = pos.getX() + 0.5, z = pos.getZ() + 0.5;
        double surface = pos.getY() + 1 + depth;
        if (random.nextInt(3) == 0) {
            level.addParticle(ModParticles.SULFUR_BUBBLES, x + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 1.1, z + (random.nextDouble() - 0.5) * 0.6,
                    0.0, 0.08, 0.0);
        }
        if (phase.isGeyser()) {
            level.addAlwaysVisibleParticle(ModParticles.GEYSER_BASE, true, x, surface, z, 0.0, 0.0, 0.0);
            for (int i = 0; i < 2; i++) {
                level.addAlwaysVisibleParticle(ModParticles.GEYSER_PLUME, true, x + (random.nextDouble() - 0.5) * 0.5, surface,
                        z + (random.nextDouble() - 0.5) * 0.5, 0.0, Math.sqrt(2.0 * 0.04 * 5.0 * depth) + random.nextDouble() * 0.1, 0.0);
            }
            if (random.nextInt(4) == 0) {
                level.addAlwaysVisibleParticle(ModParticles.GEYSER_POOF, true, x, surface + 5.0 * depth * random.nextDouble(), z, 0.0, 0.02, 0.0);
            }
        } else if (random.nextInt(4) == 0) {
            int dx = Mth.nextInt(random, -GAS_RADIUS, GAS_RADIUS), dz = Mth.nextInt(random, -GAS_RADIUS, GAS_RADIUS);
            BlockPos gas = BlockPos.containing(x + dx, surface - 1, z + dz);
            if (level.getFluidState(gas).isSource() && level.getBlockState(gas.above()).isAir()) {
                level.addParticle(ModParticles.NOXIOUS_GAS, gas.getX() + random.nextDouble(), gas.getY() + 1.0, gas.getZ() + random.nextDouble(),
                        0.0, 0.01, 0.0);
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        countdown = tag.contains("countdown") ? tag.getInt("countdown") : -1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("countdown", countdown);
    }
}
