package io.github.kestalf.futurebackport.entity.variant;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.network.FarmVariantPayload;
import io.github.kestalf.futurebackport.registry.ModAttachments;
import io.github.kestalf.futurebackport.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Gives chickens, cows and pigs their variant: from the biome when they first spawn, from a parent when bred, from the
 * egg when hatched. Clients learn the variant when they start tracking the mob.
 */
@EventBusSubscriber(modid = FutureBackport.MOD_ID)
public final class FarmVariantEvents {
    /** Set while a thrown egg hatches, so the chicks get the egg's variant instead of the biome's. */
    private static final ThreadLocal<FarmAnimalVariant> HATCHING = new ThreadLocal<>();

    public static void startHatching(ItemStack egg) {
        HATCHING.set(egg.is(ModItems.BLUE_EGG.get()) ? FarmAnimalVariant.COLD
                : egg.is(ModItems.BROWN_EGG.get()) ? FarmAnimalVariant.WARM : FarmAnimalVariant.TEMPERATE);
    }

    public static void stopHatching() {
        HATCHING.remove();
    }

    /** The egg a chicken of this variant lays. */
    public static ItemLike eggFor(Entity chicken) {
        return switch (FarmAnimalVariant.of(chicken)) {
            case TEMPERATE -> Items.EGG;
            case WARM -> ModItems.BROWN_EGG.get();
            case COLD -> ModItems.BLUE_EGG.get();
        };
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide || !FarmAnimalVariant.hasVariants(entity) || entity.hasData(ModAttachments.FARM_VARIANT)) return;
        FarmAnimalVariant hatching = HATCHING.get();
        entity.setData(ModAttachments.FARM_VARIANT, hatching != null ? hatching : FarmAnimalVariant.forBiome(event.getLevel(), entity.blockPosition()));
    }

    @SubscribeEvent
    static void onBreed(BabyEntitySpawnEvent event) {
        Entity child = event.getChild();
        if (child == null || !FarmAnimalVariant.hasVariants(child)) return;
        Entity parent = child.getRandom().nextBoolean() ? event.getParentA() : event.getParentB();
        child.setData(ModAttachments.FARM_VARIANT, FarmAnimalVariant.of(parent));
    }

    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        sync(event.getTarget(), event.getEntity() instanceof ServerPlayer player ? player : null);
    }

    private static void sync(Entity entity, @Nullable ServerPlayer player) {
        if (player == null || !FarmAnimalVariant.hasVariants(entity)) return;
        FarmAnimalVariant variant = FarmAnimalVariant.of(entity);
        if (variant != FarmAnimalVariant.TEMPERATE) PacketDistributor.sendToPlayer(player, new FarmVariantPayload(entity.getId(), variant));
    }

    private FarmVariantEvents() {
    }
}
