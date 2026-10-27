package io.github.kestalf.futurebackport.block.entity;

import com.mojang.datafixers.util.Either;
import io.github.kestalf.futurebackport.block.CreakingHeartBlock;
import io.github.kestalf.futurebackport.block.CreakingHeartState;
import io.github.kestalf.futurebackport.entity.creaking.Creaking;
import io.github.kestalf.futurebackport.particle.TrailParticleOption;
import io.github.kestalf.futurebackport.registry.ModBlockEntities;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModSounds;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/** Spawns and protects the creaking of an awake creaking heart; see {@link CreakingHeartBlock}. */
public class CreakingHeartBlockEntity extends BlockEntity {
    private static final int PLAYER_DETECTION_RANGE = 32;
    public static final int CREAKING_ROAMING_RADIUS = 32;
    private static final int DISTANCE_CREAKING_TOO_FAR = 34;
    private static final int SPAWN_RANGE_XZ = 16;
    private static final int SPAWN_RANGE_Y = 8;
    private static final int ATTEMPTS_PER_SPAWN = 5;
    private static final int UPDATE_TICKS = 20;
    private static final int UPDATE_TICKS_VARIANCE = 5;
    private static final int HURT_CALL_TOTAL_TICKS = 100;
    private static final int NUMBER_OF_HURT_CALLS = 10;
    private static final int HURT_CALL_INTERVAL = HURT_CALL_TOTAL_TICKS / NUMBER_OF_HURT_CALLS;
    private static final int HURT_CALL_PARTICLE_TICKS = 50;
    private static final int ORANGE = 0xFC7812;
    private static final int GRAY = 0x5F5F5F;

    @Nullable
    private Either<Creaking, UUID> creakingInfo;
    private long ticksExisted;
    private int ticker;
    private int emitter;
    @Nullable
    private Vec3 emitterTarget;
    private int outputSignal;

    public CreakingHeartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREAKING_HEART.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CreakingHeartBlockEntity heart) {
        heart.ticksExisted++;
        if (!(level instanceof ServerLevel serverLevel)) return;
        int signal = heart.computeAnalogOutputSignal();
        if (heart.outputSignal != signal) {
            heart.outputSignal = signal;
            level.updateNeighbourForOutputSignal(pos, ModBlocks.CREAKING_HEART.get());
        }

        if (heart.emitter > 0) {
            if (heart.emitter > HURT_CALL_PARTICLE_TICKS) {
                heart.emitParticles(serverLevel, 1, true);
                heart.emitParticles(serverLevel, 1, false);
            }
            if (heart.emitter % HURT_CALL_INTERVAL == 0 && heart.emitterTarget != null) {
                heart.getCreakingProtector().ifPresent(creaking -> heart.emitterTarget = creaking.getBoundingBox().getCenter());
                Vec3 center = Vec3.atCenterOf(pos);
                float progress = 0.2F + 0.8F * (HURT_CALL_TOTAL_TICKS - heart.emitter) / HURT_CALL_TOTAL_TICKS;
                Vec3 soundPos = center.subtract(heart.emitterTarget).scale(progress).add(heart.emitterTarget);
                float volume = heart.emitter / 2.0F / HURT_CALL_TOTAL_TICKS + 0.5F;
                serverLevel.playSound(null, BlockPos.containing(soundPos), ModSounds.event("block.creaking_heart.hurt").get(),
                        SoundSource.BLOCKS, volume, 1.0F);
            }
            heart.emitter--;
        }

        if (heart.ticker-- >= 0) return;
        heart.ticker = level.random.nextInt(UPDATE_TICKS_VARIANCE) + UPDATE_TICKS;
        BlockState updated = heart.updateCreakingState(level, state, pos);
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
            if (updated.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.UPROOTED) return;
        }

        if (heart.creakingInfo == null) {
            if (updated.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE && serverLevel.getDifficulty() != Difficulty.PEACEFUL
                    && serverLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
                Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), PLAYER_DETECTION_RANGE, false);
                if (player != null) {
                    Creaking creaking = spawnProtector(serverLevel, heart);
                    if (creaking != null) {
                        heart.setCreakingInfo(creaking);
                        creaking.playSound(ModSounds.event("entity.creaking.spawn").get(), 1.0F, 1.0F);
                        level.playSound(null, pos, ModSounds.event("block.creaking_heart.spawn").get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }
        } else {
            heart.getCreakingProtector().ifPresent(creaking -> {
                if (!CreakingHeartBlock.isNaturalNight(level) && !creaking.isPersistenceRequired()
                        || heart.distanceToCreaking() > DISTANCE_CREAKING_TOO_FAR || creaking.playerIsStuckInYou()) {
                    heart.removeProtector(null);
                }
            });
        }
    }

    private BlockState updateCreakingState(Level level, BlockState state, BlockPos pos) {
        if (!CreakingHeartBlock.hasRequiredLogs(state, level, pos) && creakingInfo == null) {
            return state.setValue(CreakingHeartBlock.STATE, CreakingHeartState.UPROOTED);
        }
        return state.setValue(CreakingHeartBlock.STATE, CreakingHeartBlock.isNaturalNight(level) ? CreakingHeartState.AWAKE : CreakingHeartState.DORMANT);
    }

    private double distanceToCreaking() {
        return getCreakingProtector().map(creaking -> Math.sqrt(creaking.distanceToSqr(Vec3.atBottomCenterOf(getBlockPos())))).orElse(0.0);
    }

    private void clearCreakingInfo() {
        creakingInfo = null;
        setChanged();
    }

    public void setCreakingInfo(Creaking creaking) {
        creakingInfo = Either.left(creaking);
        setChanged();
    }

    public void setCreakingInfo(UUID uuid) {
        creakingInfo = Either.right(uuid);
        ticksExisted = 0L;
        setChanged();
    }

    /** The linked creaking, resolving a saved UUID once the entity is loaded. */
    public Optional<Creaking> getCreakingProtector() {
        if (creakingInfo == null) return Optional.empty();
        if (creakingInfo.left().isPresent()) {
            Creaking creaking = creakingInfo.left().get();
            if (!creaking.isRemoved()) return Optional.of(creaking);
            setCreakingInfo(creaking.getUUID());
        }
        if (level instanceof ServerLevel serverLevel && creakingInfo.right().isPresent()) {
            UUID uuid = creakingInfo.right().get();
            if (serverLevel.getEntity(uuid) instanceof Creaking creaking) {
                setCreakingInfo(creaking);
                return Optional.of(creaking);
            }
            if (ticksExisted >= 30L) clearCreakingInfo();
        }
        return Optional.empty();
    }

    @Nullable
    private static Creaking spawnProtector(ServerLevel level, CreakingHeartBlockEntity heart) {
        BlockPos center = heart.getBlockPos();
        RandomSource random = level.random;
        for (int attempt = 0; attempt < ATTEMPTS_PER_SPAWN; attempt++) {
            BlockPos column = center.offset(Mth.randomBetweenInclusive(random, -SPAWN_RANGE_XZ, SPAWN_RANGE_XZ),
                    SPAWN_RANGE_Y, Mth.randomBetweenInclusive(random, -SPAWN_RANGE_XZ, SPAWN_RANGE_XZ));
            BlockPos spawnPos = findSpawnPos(level, column);
            if (spawnPos == null) continue;
            Creaking creaking = ModEntities.CREAKING.get().create(level);
            if (creaking == null) return null;
            creaking.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(creaking) || level.containsAnyLiquid(creaking.getBoundingBox())) continue;
            creaking.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.SPAWNER, null);
            level.addFreshEntityWithPassengers(creaking);
            level.gameEvent(creaking, GameEvent.ENTITY_PLACE, creaking.position());
            level.broadcastEntityEvent(creaking, Creaking.EVENT_SPAWN_PARTICLES);
            creaking.setTransient(heart);
            return creaking;
        }
        return null;
    }

    /** Walks down the column to the first non-leaves block with a sturdy top under a free space. */
    @Nullable
    private static BlockPos findSpawnPos(ServerLevel level, BlockPos top) {
        BlockPos.MutableBlockPos pos = top.mutable();
        for (int i = 0; i < SPAWN_RANGE_Y * 2; i++) {
            pos.move(Direction.DOWN);
            BlockState below = level.getBlockState(pos);
            BlockState at = level.getBlockState(pos.above());
            if (!below.is(net.minecraft.tags.BlockTags.LEAVES) && below.isFaceSturdy(level, pos, Direction.UP)
                    && at.getCollisionShape(level, pos.above()).isEmpty() && at.getFluidState().isEmpty()) {
                return pos.above().immutable();
            }
        }
        return null;
    }

    /** A protected creaking was attacked: show trails and grow resin on nearby pale oak logs. */
    public void creakingHurt() {
        if (!(level instanceof ServerLevel serverLevel) || emitter > 0) return;
        getCreakingProtector().ifPresent(creaking -> {
            emitParticles(serverLevel, 20, false);
            int count = serverLevel.getRandom().nextIntBetweenInclusive(2, 3);
            for (int i = 0; i < count; i++) {
                spreadResin().ifPresent(pos -> {
                    serverLevel.playSound(null, pos, ModSounds.event("block.resin.place").get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                    serverLevel.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(serverLevel.getBlockState(pos)));
                });
            }
            emitter = HURT_CALL_TOTAL_TICKS;
            emitterTarget = creaking.getBoundingBox().getCenter();
        });
    }

    private Optional<BlockPos> spreadResin() {
        RandomSource random = level.getRandom();
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(worldPosition);
        seen.add(worldPosition);
        int visited = 0;
        while (!queue.isEmpty() && visited++ < 64) {
            BlockPos current = queue.poll();
            int depth = current.distManhattan(worldPosition);
            boolean log = level.getBlockState(current).is(ModTags.Blocks.PALE_OAK_LOGS);
            if (log) {
                for (Direction direction : Util.shuffledCopy(Direction.values(), random)) {
                    BlockPos target = current.relative(direction);
                    BlockState state = level.getBlockState(target);
                    Direction face = direction.getOpposite();
                    if (state.isAir()) {
                        state = ModBlocks.RESIN_CLUMP.get().defaultBlockState();
                    } else if (state.is(Blocks.WATER) && state.getFluidState().isSource()) {
                        state = ModBlocks.RESIN_CLUMP.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true);
                    }
                    if (state.is(ModBlocks.RESIN_CLUMP.get()) && !MultifaceBlock.hasFace(state, face)) {
                        level.setBlock(target, state.setValue(MultifaceBlock.getFaceProperty(face), true), Block.UPDATE_ALL);
                        return Optional.of(target);
                    }
                }
            }
            if (depth >= 2 && current != worldPosition) continue;
            for (Direction direction : Util.shuffledCopy(Direction.values(), random)) {
                BlockPos next = current.relative(direction);
                if (level.getBlockState(next).is(ModTags.Blocks.PALE_OAK_LOGS) && seen.add(next)) queue.add(next);
            }
        }
        return Optional.empty();
    }

    private void emitParticles(ServerLevel level, int count, boolean towardsCreaking) {
        getCreakingProtector().ifPresent(creaking -> {
            int color = towardsCreaking ? ORANGE : GRAY;
            RandomSource random = level.random;
            for (int i = 0; i < count; i++) {
                AABB box = creaking.getBoundingBox();
                Vec3 atCreaking = new Vec3(box.minX + random.nextDouble() * box.getXsize(), box.minY + random.nextDouble() * box.getYsize(),
                        box.minZ + random.nextDouble() * box.getZsize());
                Vec3 atHeart = Vec3.atLowerCornerOf(getBlockPos()).add(random.nextDouble(), random.nextDouble(), random.nextDouble());
                Vec3 from = towardsCreaking ? atHeart : atCreaking;
                Vec3 to = towardsCreaking ? atCreaking : atHeart;
                TrailParticleOption trail = new TrailParticleOption(to, color, random.nextInt(40) + 10);
                level.sendParticles(trail, from.x, from.y, from.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        });
    }

    /** Kills the linked creaking: with a damage source it twitches and dies, otherwise it crumbles at once. */
    public void removeProtector(@Nullable DamageSource source) {
        getCreakingProtector().ifPresent(creaking -> {
            if (source == null) {
                creaking.tearDown();
            } else {
                creaking.creakingDeathEffects(source);
                creaking.setTearingDown();
                creaking.setHealth(0.0F);
            }
            clearCreakingInfo();
        });
    }

    public boolean isProtector(Creaking creaking) {
        return getCreakingProtector().map(linked -> linked == creaking).orElse(false);
    }

    public int getAnalogOutputSignal() {
        return outputSignal;
    }

    private int computeAnalogOutputSignal() {
        if (creakingInfo == null) return 0;
        double distance = Mth.clamp(distanceToCreaking(), 0.0, CREAKING_ROAMING_RADIUS) / CREAKING_ROAMING_RADIUS;
        return 15 - (int) Math.floor(distance * 15.0);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("creaking")) {
            setCreakingInfo(tag.getUUID("creaking"));
        } else {
            clearCreakingInfo();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (creakingInfo != null) {
            tag.putUUID("creaking", creakingInfo.map(Entity::getUUID, uuid -> uuid));
        }
    }
}
