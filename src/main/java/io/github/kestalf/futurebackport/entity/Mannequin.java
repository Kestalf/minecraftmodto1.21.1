package io.github.kestalf.futurebackport.entity;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Mannequin (1.21.9): a player-shaped entity for map makers, summoned with commands only. It shows a player's skin
 * ({@code profile}, a name or a full profile; slim Alex by default), wears armour and holds items, takes a pose and
 * shows a line below its name ("NPC" unless {@code description} or {@code hide_description} say otherwise). It has
 * no AI, picks nothing up and drops nothing.
 */
public class Mannequin extends LivingEntity {
    /** Outer skin layers in the bit order of {@link #HIDDEN_LAYERS}. */
    public static final List<String> LAYERS = List.of("cape", "jacket", "left_sleeve", "right_sleeve", "left_pants_leg", "right_pants_leg", "hat");
    private static final List<Pose> POSES = List.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING, Pose.FALL_FLYING, Pose.SLEEPING);
    private static final EntityDataAccessor<CompoundTag> PROFILE = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Byte> HIDDEN_LAYERS = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> LEFT_HANDED = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IMMOVABLE = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<Component>> DESCRIPTION =
            SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.OPTIONAL_COMPONENT);
    private static final EntityDataAccessor<Boolean> HIDE_DESCRIPTION = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.BOOLEAN);

    private final NonNullList<ItemStack> hands = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> armor = NonNullList.withSize(4, ItemStack.EMPTY);
    @Nullable
    private ResolvableProfile profile;

    public Mannequin(EntityType<? extends Mannequin> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PROFILE, new CompoundTag());
        builder.define(HIDDEN_LAYERS, (byte) 0);
        builder.define(LEFT_HANDED, false);
        builder.define(IMMOVABLE, false);
        builder.define(DESCRIPTION, Optional.empty());
        builder.define(HIDE_DESCRIPTION, false);
    }

    // ------------------------------------------------------------------ appearance

    @Nullable
    public ResolvableProfile getProfile() {
        return profile;
    }

    /** Sets the skin; a profile given only by name or id is looked up and filled in on the server. */
    public void setProfile(@Nullable ResolvableProfile profile) {
        this.profile = profile;
        CompoundTag tag = new CompoundTag();
        if (profile != null) ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).result().ifPresent(t -> tag.put("profile", t));
        entityData.set(PROFILE, tag);
        if (profile != null && !profile.isResolved() && level() instanceof ServerLevel server) {
            profile.resolve().thenAcceptAsync(resolved -> {
                if (!isRemoved() && this.profile == profile) setProfile(resolved);
            }, server.getServer());
        }
    }

    public boolean isLayerVisible(String layer) {
        return (entityData.get(HIDDEN_LAYERS) & (1 << LAYERS.indexOf(layer))) == 0;
    }

    /** The line shown below the name, or null when it is hidden. */
    @Nullable
    public Component getDescriptionLine() {
        if (entityData.get(HIDE_DESCRIPTION)) return null;
        return entityData.get(DESCRIPTION).orElseGet(() -> Component.translatable(getType().getDescriptionId() + ".label"));
    }

    public boolean isImmovable() {
        return entityData.get(IMMOVABLE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (PROFILE.equals(key) && level().isClientSide) {
            CompoundTag tag = entityData.get(PROFILE);
            profile = tag.contains("profile") ? ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, tag.get("profile")).result().orElse(null) : null;
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return switch (pose) {
            case CROUCHING -> EntityDimensions.scalable(0.6F, 1.5F).withEyeHeight(1.27F);
            case SWIMMING, FALL_FLYING -> EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F);
            case SLEEPING -> EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
            default -> super.getDefaultDimensions(pose);
        };
    }

    // ------------------------------------------------------------------ equipment

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return armor;
    }

    @Override
    public Iterable<ItemStack> getHandSlots() {
        return hands;
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot.getType()) {
            case HAND -> hands.get(slot.getIndex());
            case HUMANOID_ARMOR -> armor.get(slot.getIndex());
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        verifyEquippedItem(stack);
        switch (slot.getType()) {
            case HAND -> onEquipItem(slot, hands.set(slot.getIndex(), stack), stack);
            case HUMANOID_ARMOR -> onEquipItem(slot, armor.set(slot.getIndex(), stack), stack);
            default -> {
            }
        }
    }

    /** Dispensers can put armour on it. */
    @Override
    public boolean canTakeItem(ItemStack stack) {
        EquipmentSlot slot = getEquipmentSlotForItem(stack);
        return getItemBySlot(slot).isEmpty();
    }

    @Override
    public HumanoidArm getMainArm() {
        return entityData.get(LEFT_HANDED) ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    // ------------------------------------------------------------------ physics

    @Override
    public boolean isPushable() {
        return !isImmovable() && super.isPushable();
    }

    @Override
    public void push(double x, double y, double z) {
        if (!isImmovable()) super.push(x, y, z);
    }

    @Override
    public void knockback(double strength, double x, double z) {
        if (!isImmovable()) super.knockback(strength, x, z);
    }

    @Override
    protected float tickHeadTurn(float yRot, float animStep) {
        yBodyRotO = yRotO;
        yBodyRot = getYRot();
        return 0.0F;
    }

    @Override
    public void setYBodyRot(float yRot) {
        yBodyRotO = yRotO = yRot;
        yHeadRotO = yHeadRot = yRot;
    }

    @Override
    public void setYHeadRot(float yRot) {
        yBodyRotO = yRotO = yRot;
        yHeadRotO = yHeadRot = yRot;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag armorTag = new ListTag();
        for (ItemStack stack : armor) armorTag.add(stack.saveOptional(registryAccess()));
        tag.put("ArmorItems", armorTag);
        ListTag handsTag = new ListTag();
        for (ItemStack stack : hands) handsTag.add(stack.saveOptional(registryAccess()));
        tag.put("HandItems", handsTag);
        if (profile != null) ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).result().ifPresent(t -> tag.put("profile", t));
        ListTag hidden = new ListTag();
        for (String layer : LAYERS) {
            if (!isLayerVisible(layer)) hidden.add(StringTag.valueOf(layer));
        }
        tag.put("hidden_layers", hidden);
        tag.putString("main_hand", entityData.get(LEFT_HANDED) ? "left" : "right");
        tag.putString("pose", getPose().name().toLowerCase(Locale.ROOT));
        tag.putBoolean("immovable", isImmovable());
        entityData.get(DESCRIPTION).ifPresent(description -> ComponentSerialization.CODEC
                .encodeStart(registryAccess().createSerializationContext(NbtOps.INSTANCE), description).result()
                .ifPresent(t -> tag.put("description", t)));
        tag.putBoolean("hide_description", entityData.get(HIDE_DESCRIPTION));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readItems(tag.getList("ArmorItems", Tag.TAG_COMPOUND), armor);
        readItems(tag.getList("HandItems", Tag.TAG_COMPOUND), hands);
        if (tag.contains("profile")) setProfile(ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, tag.get("profile")).result().orElse(null));
        int hidden = 0;
        for (Tag layer : tag.getList("hidden_layers", Tag.TAG_STRING)) {
            int index = LAYERS.indexOf(layer.getAsString());
            if (index >= 0) hidden |= 1 << index;
        }
        entityData.set(HIDDEN_LAYERS, (byte) hidden);
        if (tag.contains("main_hand")) entityData.set(LEFT_HANDED, tag.getString("main_hand").equals("left"));
        if (tag.contains("pose")) {
            String name = tag.getString("pose");
            POSES.stream().filter(pose -> pose.name().toLowerCase(Locale.ROOT).equals(name)).findFirst().ifPresent(this::setPose);
        }
        entityData.set(IMMOVABLE, tag.getBoolean("immovable"));
        entityData.set(DESCRIPTION, tag.contains("description") ? ComponentSerialization.CODEC
                .parse(registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get("description")).result() : Optional.empty());
        entityData.set(HIDE_DESCRIPTION, tag.getBoolean("hide_description"));
    }

    private void readItems(ListTag list, NonNullList<ItemStack> into) {
        for (int i = 0; i < into.size() && i < list.size(); i++) into.set(i, ItemStack.parseOptional(registryAccess(), list.getCompound(i)));
    }
}
