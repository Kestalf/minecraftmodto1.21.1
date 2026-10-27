package io.github.kestalf.futurebackport.worldgen;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.worldgen.tree.CreakingHeartDecorator;
import io.github.kestalf.futurebackport.worldgen.tree.PaleMossDecorator;
import io.github.kestalf.futurebackport.worldgen.tree.PoplarFoliagePlacer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacerType;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecoratorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Code-defined world generation types (foliage placers, tree decorators, features). */
public final class ModWorldgen {
    public static final DeferredRegister<FoliagePlacerType<?>> FOLIAGE_PLACERS = DeferredRegister.create(Registries.FOLIAGE_PLACER_TYPE, FutureBackport.MOD_ID);
    public static final DeferredRegister<TreeDecoratorType<?>> TREE_DECORATORS = DeferredRegister.create(Registries.TREE_DECORATOR_TYPE, FutureBackport.MOD_ID);

    public static final DeferredHolder<FoliagePlacerType<?>, FoliagePlacerType<PoplarFoliagePlacer>> POPLAR_FOLIAGE_PLACER =
            FOLIAGE_PLACERS.register("poplar_foliage_placer", () -> new FoliagePlacerType<>(PoplarFoliagePlacer.CODEC));
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<PaleMossDecorator>> PALE_MOSS =
            TREE_DECORATORS.register("pale_moss", () -> new TreeDecoratorType<>(PaleMossDecorator.CODEC));
    public static final DeferredHolder<TreeDecoratorType<?>, TreeDecoratorType<CreakingHeartDecorator>> CREAKING_HEART =
            TREE_DECORATORS.register("creaking_heart", () -> new TreeDecoratorType<>(CreakingHeartDecorator.CODEC));

    public static void register(IEventBus modBus) {
        FOLIAGE_PLACERS.register(modBus);
        TREE_DECORATORS.register(modBus);
    }

    private ModWorldgen() {
    }
}
