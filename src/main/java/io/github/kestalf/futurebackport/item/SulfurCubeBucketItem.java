package io.github.kestalf.futurebackport.item;

import io.github.kestalf.futurebackport.entity.sulfur.SulfurCube;
import io.github.kestalf.futurebackport.registry.ModEntities;
import io.github.kestalf.futurebackport.registry.ModItems;
import io.github.kestalf.futurebackport.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.List;

/** Bucket of sulfur cube: holds a large cube (with its absorbed block) and lets it out where it is used. */
public class SulfurCubeBucketItem extends Item {
    private static final String BLOCK = "block";

    public SulfurCubeBucketItem(Properties properties) {
        super(properties);
    }

    /** A bucket holding the given cube; the caller removes the cube. */
    public static ItemStack fill(SulfurCube cube) {
        ItemStack bucket = new ItemStack(ModItems.SULFUR_CUBE_BUCKET.get());
        Bucketable.saveDefaultDataToBucketTag(cube, bucket);
        if (cube.hasBlock()) {
            CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> tag.put(BLOCK, cube.getBlock().save(cube.registryAccess())));
        }
        return bucket;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
        if (level instanceof ServerLevel server) {
            ItemStack stack = context.getItemInHand();
            Player player = context.getPlayer();
            SulfurCube cube = ModEntities.SULFUR_CUBE.get().create(server, EntityType.createDefaultStackConfig(server, stack, player), pos,
                    MobSpawnType.BUCKET, true, false);
            if (cube != null) {
                CompoundTag tag = stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag();
                Bucketable.loadDefaultDataFromBucketTag(cube, tag);
                if (tag.contains(BLOCK)) {
                    ItemStack.parse(server.registryAccess(), tag.getCompound(BLOCK)).ifPresent(block -> cube.setItemSlot(EquipmentSlot.BODY, block));
                }
                cube.setFromBucket(true);
                cube.setPersistenceRequired();
                server.addFreshEntityWithPassengers(cube);
                level.playSound(null, pos, ModSounds.event("item.bucket.empty_sulfur_cube").get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                level.gameEvent(player, GameEvent.ENTITY_PLACE, pos);
            }
            if (player != null) {
                player.awardStat(Stats.ITEM_USED.get(this));
                player.setItemInHand(context.getHand(), BucketItem.getEmptySuccessItem(stack, player));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        CustomData data = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        if (data == null || context.registries() == null) return;
        CompoundTag tag = data.copyTag();
        if (!tag.contains(BLOCK)) return;
        ItemStack.parse(context.registries(), tag.getCompound(BLOCK)).ifPresent(block -> lines.add(
                Component.translatable("entity.futurebackport.sulfur_cube.content", block.getHoverName()).withStyle(ChatFormatting.GRAY)));
    }
}
