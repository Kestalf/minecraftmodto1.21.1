package io.github.kestalf.futurebackport.item;

import io.github.kestalf.futurebackport.entity.Cushion;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/** Places a cushion in the block space the clicked face points to (or in the clicked block if it is a partial one). */
public class CushionItem extends Item {
    private final DyeColor color;

    public CushionItem(DyeColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public DyeColor color() {
        return color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        float yRot = context.getPlayer() != null ? context.getPlayer().getYRot() + 180.0F : 0.0F;
        Cushion cushion = Cushion.place(level, clicked, color, yRot);
        if (cushion == null) cushion = Cushion.place(level, clicked.relative(context.getClickedFace()), color, yRot);
        if (cushion == null) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            level.addFreshEntity(cushion);
            level.playSound(null, cushion.getX(), cushion.getY(), cushion.getZ(),
                    io.github.kestalf.futurebackport.registry.ModSounds.event("entity.cushion.place").get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(context.getPlayer(), GameEvent.ENTITY_PLACE, cushion.position());
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
