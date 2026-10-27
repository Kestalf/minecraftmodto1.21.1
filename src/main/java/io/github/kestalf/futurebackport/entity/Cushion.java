package io.github.kestalf.futurebackport.entity;

import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModItems;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Cushion: a seat entity resting on whatever surface is inside or below its block space. It breaks when hit, pushed
 * by a piston or left without support, and always drops itself.
 */
public class Cushion extends Entity {
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(Cushion.class, EntityDataSerializers.INT);
    private static final int SUPPORT_CHECK_INTERVAL = 100;
    private BlockPos blockPos = BlockPos.ZERO;
    private int lavaTicks;

    public Cushion(EntityType<? extends Cushion> type, Level level) {
        super(type, level);
    }

    /** A cushion in the block space {@code pos}, or null if nothing there supports it. */
    @Nullable
    public static Cushion place(Level level, BlockPos pos, DyeColor color, float yRot) {
        Double surface = surfaceHeight(level, pos);
        if (surface == null) return null;
        Cushion cushion = new Cushion(ModEntities.CUSHION.get(), level);
        cushion.blockPos = pos.immutable();
        cushion.setColor(color);
        cushion.moveTo(pos.getX() + 0.5, surface, pos.getZ() + 0.5, Direction.fromYRot(yRot).toYRot(), 0.0F);
        if (!level.getEntitiesOfClass(Cushion.class, cushion.getBoundingBox().deflate(0.01)).isEmpty()) return null;
        return cushion;
    }

    /** Top of the collision shape inside the block space, else the top of the block below; null without support. */
    @Nullable
    private static Double surfaceHeight(Level level, BlockPos pos) {
        VoxelShape inside = level.getBlockState(pos).getCollisionShape(level, pos);
        if (!inside.isEmpty()) {
            double top = inside.max(Direction.Axis.Y);
            return top < 1.0 ? pos.getY() + top : null;
        }
        VoxelShape below = level.getBlockState(pos.below()).getCollisionShape(level, pos.below());
        return !below.isEmpty() && below.max(Direction.Axis.Y) >= 1.0 ? (double) pos.getY() : null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, DyeColor.WHITE.getId());
    }

    public DyeColor getColor() {
        return DyeColor.byId(entityData.get(COLOR));
    }

    public void setColor(DyeColor color) {
        entityData.set(COLOR, color.getId());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount % SUPPORT_CHECK_INTERVAL == 0 && surfaceHeight(level(), blockPos) == null) {
            breakCushion(null);
            return;
        }
        if (isInLava() && ++lavaTicks > 60) breakCushion(null);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || isVehicle()) return InteractionResult.PASS;
        if (!level().isClientSide) {
            player.startRiding(this);
            playSound(sound("sit"), 1.0F, 1.0F);
            gameEvent(GameEvent.ENTITY_INTERACT, player);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide && !isRemoved()) playSound(sound("get_up"), 1.0F, 1.0F);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (!level().isClientSide && !isRemoved()) breakCushion(source.getEntity());
        return true;
    }

    @Override
    public void move(MoverType type, Vec3 movement) {
        if (type == MoverType.PISTON) {
            if (!level().isClientSide && !isRemoved()) breakCushion(null);
            return;
        }
        super.move(type, movement);
    }

    @Override
    public void push(double x, double y, double z) {
    }

    private void breakCushion(@Nullable Entity breaker) {
        playSound(sound("break"), 1.0F, 1.0F);
        gameEvent(GameEvent.ENTITY_DIE, breaker);
        if (level() instanceof ServerLevel server && server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)
                && !(breaker instanceof Player player && player.getAbilities().instabuild)) {
            spawnAtLocation(new ItemStack(ModItems.CUSHIONS.get(getColor()).get()));
        }
        ejectPassengers();
        discard();
    }

    static SoundEvent sound(String name) {
        return ModSounds.event("entity.cushion." + name).get();
    }

    @Override
    @Nullable
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.CUSHIONS.get(getColor()).get());
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0, dimensions.height(), 0.0);
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        NbtUtils.readBlockPos(tag, "block_pos").ifPresent(pos -> blockPos = pos);
        setColor(DyeColor.byName(tag.getString("color"), DyeColor.WHITE));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("block_pos", NbtUtils.writeBlockPos(blockPos));
        tag.putString("color", getColor().getSerializedName());
    }
}
