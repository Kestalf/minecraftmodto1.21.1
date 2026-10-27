package io.github.kestalf.futurebackport.item;

import io.github.kestalf.futurebackport.registry.ModAttachments;
import io.github.kestalf.futurebackport.registry.ModParticles;
import io.github.kestalf.futurebackport.registry.ModSounds;
import io.github.kestalf.futurebackport.registry.ModTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Golden dandelion: using it on a baby mob stops (or resumes) its growth. */
public class GoldenDandelionItem extends BlockItem {
    public GoldenDandelionItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean canAgeLock(LivingEntity target) {
        return target instanceof AgeableMob mob && mob.isBaby()
                && (!(target instanceof Enemy) || target instanceof Hoglin)
                && !(target instanceof AbstractVillager)
                && !target.getType().is(ModTags.Entities.CANNOT_BE_AGE_LOCKED);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!canAgeLock(target)) return InteractionResult.PASS;
        if (target.level() instanceof ServerLevel level) {
            boolean locked = !target.getData(ModAttachments.AGE_LOCKED);
            target.setData(ModAttachments.AGE_LOCKED, locked);
            level.sendParticles(locked ? ModParticles.PAUSE_MOB_GROWTH : ModParticles.RESET_MOB_GROWTH,
                    target.getX(), target.getY(0.5), target.getZ(), 7, target.getBbWidth() / 2, target.getBbHeight() / 3,
                    target.getBbWidth() / 2, 0.0);
            level.playSound(null, target, ModSounds.event(locked ? "item.golden_dandelion.use" : "item.golden_dandelion.unuse").get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            stack.consume(1, player);
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }
}
