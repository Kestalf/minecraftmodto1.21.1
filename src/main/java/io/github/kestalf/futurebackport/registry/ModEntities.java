package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.ghast.HappyGhast;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, FutureBackport.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<HappyGhast>> HAPPY_GHAST = ENTITIES.register("happy_ghast",
            () -> EntityType.Builder.of(HappyGhast::new, MobCategory.CREATURE).sized(4.0F, 4.0F).eyeHeight(2.6F)
                    .clientTrackingRange(10).build("happy_ghast"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HAPPY_GHAST.get(), HappyGhast.createAttributes().build());
    }

    private ModEntities() {
    }
}
