package io.github.kestalf.futurebackport.entity.golem;

import io.github.kestalf.futurebackport.block.CopperGolemPose;
import io.github.kestalf.futurebackport.block.CopperGolemStatueBlock;
import io.github.kestalf.futurebackport.block.entity.CopperGolemStatueBlockEntity;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WeatheringCopper.WeatherState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Copper golem: carries items from copper chests to nearby wooden chests ({@link TransportItemsGoal}), oxidizes over
 * time unless waxed and finally turns into a {@link CopperGolemStatueBlock}.
 */
public class CopperGolem extends AbstractGolem {
    private static final EntityDataAccessor<Integer> WEATHER_STATE = SynchedEntityData.defineId(CopperGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> INTERACTION = SynchedEntityData.defineId(CopperGolem.class, EntityDataSerializers.INT);
    /** Oxidizing to the next stage takes 420 to 460 minutes. */
    private static final int WEATHERING_MIN_TICKS = 420 * 1200;
    private static final int WEATHERING_MAX_TICKS = 460 * 1200;
    private static final float STATUE_CHANCE = 0.0058F;
    private static final byte EVENT_SPIN = 70;

    public final AnimationState spinAnimationState = new AnimationState();
    public final AnimationState interactionAnimationState = new AnimationState();
    private boolean waxed;
    private long nextWeatheringTick = -1L;
    private int spinCooldown = 200;
    private int lastInteraction;

    public CopperGolem(EntityType<? extends CopperGolem> type, Level level) {
        super(type, level);
        if (getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
            navigation.setCanPassDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return AbstractGolem.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PanicGoal(this, 1.5));
        goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        goalSelector.addGoal(2, new TransportItemsGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WEATHER_STATE, WeatherState.UNAFFECTED.ordinal());
        builder.define(INTERACTION, Interaction.NONE.ordinal());
    }

    public WeatherState getWeatherState() {
        return WeatherState.values()[entityData.get(WEATHER_STATE)];
    }

    public void setWeatherState(WeatherState state) {
        entityData.set(WEATHER_STATE, state.ordinal());
    }

    public boolean isWaxed() {
        return waxed;
    }

    public Interaction getInteraction() {
        return Interaction.values()[entityData.get(INTERACTION)];
    }

    void setInteraction(Interaction interaction) {
        entityData.set(INTERACTION, interaction.ordinal());
    }

    // ------------------------------------------------------------------ ticking: oxidation, statues, idle spins

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            int interaction = entityData.get(INTERACTION);
            if (interaction != lastInteraction) {
                lastInteraction = interaction;
                interactionAnimationState.stop();
            }
            interactionAnimationState.animateWhen(interaction != Interaction.NONE.ordinal(), tickCount);
            return;
        }
        ServerLevel level = (ServerLevel) level();
        long time = level.getGameTime();
        if (nextWeatheringTick < 0L) {
            nextWeatheringTick = time + Mth.nextInt(random, WEATHERING_MIN_TICKS, WEATHERING_MAX_TICKS);
        }
        WeatherState state = getWeatherState();
        if (!waxed) {
            if (state != WeatherState.OXIDIZED && time >= nextWeatheringTick) {
                setWeatherState(WeatherState.values()[state.ordinal() + 1]);
                nextWeatheringTick = time + Mth.nextInt(random, WEATHERING_MIN_TICKS, WEATHERING_MAX_TICKS);
            } else if (state == WeatherState.OXIDIZED && random.nextFloat() < STATUE_CHANCE && canBecomeStatue()) {
                becomeStatue(level);
                return;
            }
        }
        if (getInteraction() == Interaction.NONE && getNavigation().isDone() && --spinCooldown <= 0) {
            spinCooldown = 400 + random.nextInt(800);
            level.broadcastEntityEvent(this, EVENT_SPIN);
            playSound(sound("spin"), 1.0F, 1.0F);
        }
    }

    private boolean canBecomeStatue() {
        BlockPos pos = blockPosition();
        if (level().isOutsideBuildHeight(pos)) return false;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(getBoundingBox().minX, getBoundingBox().minY, getBoundingBox().minZ),
                BlockPos.containing(getBoundingBox().maxX, getBoundingBox().maxY, getBoundingBox().maxZ))) {
            if (!level().getBlockState(p).isAir()) return false;
        }
        return true;
    }

    private void becomeStatue(ServerLevel level) {
        BlockPos pos = blockPosition();
        ItemStack held = getMainHandItem();
        if (!held.isEmpty()) {
            spawnAtLocation(held);
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
        CopperGolemPose pose = CopperGolemPose.values()[random.nextInt(CopperGolemPose.values().length)];
        BlockState statue = ModBlocks.COPPER.statues.get(WeatherState.OXIDIZED, false).defaultBlockState()
                .setValue(CopperGolemStatueBlock.FACING, getDirection()).setValue(CopperGolemStatueBlock.POSE, pose);
        level.setBlock(pos, statue, 3);
        if (level.getBlockEntity(pos) instanceof CopperGolemStatueBlockEntity entity && hasCustomName()) {
            entity.setCustomName(getCustomName());
        }
        level.playSound(null, pos, ModSounds.event("entity.copper_golem_become_statue").get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.gameEvent(this, GameEvent.BLOCK_PLACE, pos);
        discard();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SPIN) {
            spinAnimationState.start(tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ interactions: wax, scrape, lightning

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.HONEYCOMB) && !waxed) {
            if (!level().isClientSide) {
                waxed = true;
                stack.consume(1, player);
                level().levelEvent(player, 3003, blockPosition(), 0);
                gameEvent(GameEvent.ENTITY_INTERACT, player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(ItemTags.AXES) && (waxed || getWeatherState() != WeatherState.UNAFFECTED)) {
            if (!level().isClientSide) {
                if (waxed) {
                    waxed = false;
                    level().playSound(null, this, SoundEvents.AXE_WAX_OFF, SoundSource.PLAYERS, 1.0F, 1.0F);
                    spawnParticles(ParticleTypes.WAX_OFF);
                } else {
                    setWeatherState(WeatherState.values()[getWeatherState().ordinal() - 1]);
                    nextWeatheringTick = -1L;
                    level().playSound(null, this, SoundEvents.AXE_SCRAPE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    spawnParticles(ParticleTypes.SCRAPE);
                }
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                gameEvent(GameEvent.ENTITY_INTERACT, player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void spawnParticles(ParticleOptions particle) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(particle, getX(), getY(0.5), getZ(), 12, getBbWidth() / 2, getBbHeight() / 2, getBbWidth() / 2, 1.0);
        }
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        if (!waxed && getWeatherState() != WeatherState.UNAFFECTED) {
            setWeatherState(WeatherState.values()[getWeatherState().ordinal() - 1]);
            nextWeatheringTick = -1L;
        }
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        ItemStack held = getMainHandItem();
        if (!held.isEmpty()) {
            spawnAtLocation(held);
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    // ------------------------------------------------------------------ sounds

    /** {@code entity.copper_golem[_weathered|_oxidized].<name>}; exposed golems sound like fresh ones. */
    SoundEvent sound(String name) {
        String prefix = switch (getWeatherState()) {
            case UNAFFECTED, EXPOSED -> "entity.copper_golem.";
            case WEATHERED -> "entity.copper_golem_weathered.";
            case OXIDIZED -> "entity.copper_golem_oxidized.";
        };
        return ModSounds.event(prefix + name).get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return sound("hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return sound("death");
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(sound("step"), 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ persistence

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("weather_state", getWeatherState().ordinal());
        tag.putBoolean("waxed", waxed);
        tag.putLong("next_weather_age", nextWeatheringTick);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setWeatherState(WeatherState.values()[Mth.clamp(tag.getInt("weather_state"), 0, 3)]);
        waxed = tag.getBoolean("waxed");
        nextWeatheringTick = tag.contains("next_weather_age") ? tag.getLong("next_weather_age") : -1L;
    }

    /** Spawns a golem built from a copper block of the given stage (see {@code CopperGolemBuilding}). */
    @Nullable
    public static CopperGolem spawnBuilt(ServerLevel level, Vec3 pos, float yRot, WeatherState state) {
        CopperGolem golem = io.github.kestalf.futurebackport.registry.ModEntities.COPPER_GOLEM.get().create(level);
        if (golem == null) return null;
        golem.moveTo(pos.x, pos.y, pos.z, yRot, 0.0F);
        golem.setYHeadRot(yRot);
        golem.setYBodyRot(yRot);
        golem.setWeatherState(state);
        level.addFreshEntity(golem);
        golem.playSound(ModSounds.event("entity.copper_golem.spawn").get(), 1.0F, 1.0F);
        return golem;
    }

    /** What the golem is doing at a chest; drives the arm animations. */
    public enum Interaction {
        NONE,
        TAKING,
        TAKING_FAILED,
        PUTTING,
        PUTTING_FAILED
    }
}
