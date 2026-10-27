package io.github.kestalf.futurebackport.block.entity;

import io.github.kestalf.futurebackport.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Remembers the custom name of the golem a statue was made from (and gives it back when it is reanimated). */
public class CopperGolemStatueBlockEntity extends BlockEntity {
    @Nullable
    private Component customName;

    public CopperGolemStatueBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPER_GOLEM_STATUE.get(), pos, state);
    }

    @Nullable
    public Component getCustomName() {
        return customName;
    }

    public void setCustomName(@Nullable Component name) {
        customName = name;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        customName = tag.contains("CustomName", 8) ? parseCustomNameSafe(tag.getString("CustomName"), registries) : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (customName != null) tag.putString("CustomName", Component.Serializer.toJson(customName, registries));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        customName = input.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        builder.set(DataComponents.CUSTOM_NAME, customName);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("CustomName");
    }
}
