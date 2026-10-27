package io.github.kestalf.futurebackport.registry;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.entity.creaking.Creaking;
import io.github.kestalf.futurebackport.entity.ghast.HappyGhast;
import io.github.kestalf.futurebackport.entity.golem.CopperGolem;
import io.github.kestalf.futurebackport.entity.nautilus.AbstractNautilus;
import io.github.kestalf.futurebackport.entity.nautilus.Nautilus;
import io.github.kestalf.futurebackport.entity.nautilus.ZombieNautilus;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
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
    public static final DeferredHolder<EntityType<?>, EntityType<Creaking>> CREAKING = ENTITIES.register("creaking",
            () -> EntityType.Builder.of(Creaking::new, MobCategory.MONSTER).sized(0.9F, 2.7F).eyeHeight(2.3F)
                    .clientTrackingRange(8).build("creaking"));
    public static final DeferredHolder<EntityType<?>, EntityType<CopperGolem>> COPPER_GOLEM = ENTITIES.register("copper_golem",
            () -> EntityType.Builder.of(CopperGolem::new, MobCategory.MISC).sized(0.49F, 0.98F).eyeHeight(0.8125F)
                    .clientTrackingRange(10).build("copper_golem"));
    public static final DeferredHolder<EntityType<?>, EntityType<Nautilus>> NAUTILUS = ENTITIES.register("nautilus",
            () -> EntityType.Builder.of(Nautilus::new, MobCategory.WATER_CREATURE).sized(0.875F, 0.95F).eyeHeight(0.6F)
                    .passengerAttachments(0.95F).clientTrackingRange(10).build("nautilus"));
    public static final DeferredHolder<EntityType<?>, EntityType<ZombieNautilus>> ZOMBIE_NAUTILUS = ENTITIES.register("zombie_nautilus",
            () -> EntityType.Builder.of(ZombieNautilus::new, MobCategory.MONSTER).sized(0.875F, 0.95F).eyeHeight(0.6F)
                    .passengerAttachments(0.95F).clientTrackingRange(10).build("zombie_nautilus"));

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(HAPPY_GHAST.get(), HappyGhast.createAttributes().build());
        event.put(CREAKING.get(), Creaking.createAttributes().build());
        event.put(COPPER_GOLEM.get(), CopperGolem.createAttributes().build());
        event.put(NAUTILUS.get(), AbstractNautilus.createAttributes().build());
        event.put(ZOMBIE_NAUTILUS.get(), AbstractNautilus.createAttributes().build());
    }

    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(NAUTILUS.get(), SpawnPlacementTypes.IN_WATER, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AbstractNautilus::checkSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private ModEntities() {
    }
}
