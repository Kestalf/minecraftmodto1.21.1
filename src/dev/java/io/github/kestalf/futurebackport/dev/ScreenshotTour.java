package io.github.kestalf.futurebackport.dev;

import io.github.kestalf.futurebackport.FutureBackport;
import io.github.kestalf.futurebackport.registry.ModBlocks;
import io.github.kestalf.futurebackport.registry.ModCreativeTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Development helper (not part of the mod jar): when started with {@code -Dfuturebackport.tour=true} it builds a
 * gallery of all mod blocks in the loaded world, takes screenshots of it and of the creative tab, then quits.
 * Used to check rendering on a headless machine (see the "tour" run in build.gradle).
 */
@EventBusSubscriber(modid = FutureBackport.MOD_ID, value = Dist.CLIENT)
public final class ScreenshotTour {
    private static final String SCENARIOS = System.getProperty("futurebackport.tour", "");
    private static final boolean ENABLED = !SCENARIOS.isEmpty() && !SCENARIOS.equals("false");

    private static boolean scenario(String name) {
        return SCENARIOS.equals("true") || SCENARIOS.equals("all") || java.util.Arrays.asList(SCENARIOS.split(",")).contains(name);
    }
    private static final int PER_ROW = 8;
    private static final BlockPos ORIGIN = new BlockPos(0, 250, 0);
    private static final List<Consumer<Minecraft>> STEPS = new ArrayList<>();
    private static int waitTicks = -1;
    private static int step;
    /** Work queued on the integrated server by the current step; the tour waits until it is done. */
    private static java.util.concurrent.CompletableFuture<?> pending = java.util.concurrent.CompletableFuture.completedFuture(null);
    /** Runs on every client tick while set (e.g. to keep pushing a test mob). */
    private static Runnable everyTick;

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        if (waitTicks < 0) {
            buildPlan(mc);
            waitTicks = 100;
            return;
        }
        if (everyTick != null) everyTick.run();
        if (!pending.isDone() || --waitTicks > 0) return;
        if (step >= STEPS.size()) {
            mc.stop();
            return;
        }
        STEPS.get(step++).accept(mc);
        waitTicks = 60;
    }

    private static void buildPlan(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        List<Block> blocks = ModBlocks.BLOCKS.getEntries().stream().map(h -> (Block) h.get()).toList();
        int rows = (blocks.size() + PER_ROW - 1) / PER_ROW;
        STEPS.add(m -> pending = server.submit(() -> {
            exec(server, "gamerule randomTickSpeed 0");
            exec(server, "gamerule doDaylightCycle false");
            exec(server, "gamerule doWeatherCycle false");
            exec(server, "gamerule doMobSpawning false");
            exec(server, "difficulty easy");
            exec(server, "time set 6000");
            exec(server, "weather clear");
        }));
        if (scenario("gallery")) addGallery(server, blocks, rows);
        if (scenario("spear")) addSpearScenario(server);
        if (scenario("ghast")) addGhastScenario(server);
        if (scenario("creative")) addCreativeScenario(server);
    }

    private static void addGallery(MinecraftServer server, List<Block> blocks, int rows) {
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            for (int x = -3; x <= PER_ROW * 2 + 2; x++) {
                for (int z = -12; z <= rows * 3 + 2; z++) {
                    level.setBlock(ORIGIN.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 1; y < 12; y++) level.setBlock(ORIGIN.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            for (int i = 0; i < blocks.size(); i++) {
                BlockPos pos = ORIGIN.offset((i % PER_ROW) * 2, 1, (i / PER_ROW) * 3);
                BlockState state = blocks.get(i).defaultBlockState();
                if (state.hasProperty(LeavesBlock.PERSISTENT)) state = state.setValue(LeavesBlock.PERSISTENT, true);
                level.setBlock(pos, state, 2);
                if (state.getBlock() instanceof DoorBlock) {
                    level.setBlock(pos.above(), state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
                }
            }
        }));
        STEPS.add(m -> {
            run(server, "gamemode spectator " + m.player.getGameProfile().getName());
            m.options.hideGui = true;
        });
        for (int row = 0; row < rows; row += 2) {
            final int r = row;
            STEPS.add(m -> run(server, String.format("tp %s %.1f %.1f %.1f 0 40", m.player.getGameProfile().getName(),
                    ORIGIN.getX() + PER_ROW - 0.5, ORIGIN.getY() + 5.5, ORIGIN.getZ() + r * 3 - 4.5)));
            STEPS.add(m -> shot(m, "gallery_" + String.format("%02d", r / 2)));
        }
    }

    private static void addCreativeScenario(MinecraftServer server) {
        STEPS.add(m -> {
            m.options.hideGui = false;
            run(server, "gamemode creative " + m.player.getGameProfile().getName());
        });
        STEPS.add(ScreenshotTour::openCreativeTab);
        for (int page = 0; page <= 6; page++) {
            final float scroll = page / 6.0F;
            STEPS.add(m -> {
                if (m.screen instanceof CreativeModeInventoryScreen screen) screen.getMenu().scrollTo(scroll);
            });
            STEPS.add(m -> shot(m, "creative_" + Math.round(scroll * 6)));
        }
        STEPS.add(m -> m.setScreen(null));
    }

    /**
     * Spear checks: a husk charging a pig (server-side charge logic) and the local player jabbing a cow through the
     * real attack key (client input, network payload and server jab), plus first/third person screenshots.
     */
    private static void addSpearScenario(MinecraftServer server) {
        BlockPos base = ORIGIN.offset(-40, 0, 0);
        int[] ids = new int[3];
        float[] health = new float[2];
        STEPS.add(m -> run(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 0 60",
                m.player.getGameProfile().getName(), base.getX(), base.getY() + 12, base.getZ() - 4)));
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            for (int x = -6; x <= 6; x++) {
                for (int z = -4; z <= 16; z++) {
                    level.setBlock(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 1; y < 6; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            net.minecraft.world.entity.npc.Villager pig = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
            pig.moveTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 9.5, 180, 0);
            pig.setNoAi(true);
            pig.setPersistenceRequired();
            level.addFreshEntity(pig);
            net.minecraft.world.entity.monster.Husk husk = net.minecraft.world.entity.EntityType.HUSK.create(level);
            husk.moveTo(base.getX() + 0.5, base.getY() + 1, base.getZ() + 0.5, 0, 0);
            husk.setYHeadRot(0);
            husk.setNoAi(true);
            husk.setPersistenceRequired();
            husk.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(io.github.kestalf.futurebackport.registry.ModItems.IRON_SPEAR.get()));
            level.addFreshEntity(husk);
            ids[0] = pig.getId();
            ids[1] = husk.getId();
            health[0] = pig.getHealth();
        }));
        int[] pushes = {0};
        STEPS.add(m -> everyTick = () -> server.execute(() -> {
            var husk = server.overworld().getEntity(ids[1]);
            if (husk instanceof net.minecraft.world.entity.LivingEntity living && pushes[0]++ < 30) {
                // No-AI mobs do not travel on their own: move it and keep the velocity the charge reads.
                if (!living.isUsingItem()) living.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
                living.setDeltaMovement(0, 0, 0.5);
                living.move(net.minecraft.world.entity.MoverType.SELF, living.getDeltaMovement());
            }
        }));
        STEPS.add(m -> {
            everyTick = null;
            pending = server.submit(() -> {
                var pig = (net.minecraft.world.entity.LivingEntity) server.overworld().getEntity(ids[0]);
                var husk = server.overworld().getEntity(ids[1]);
                FutureBackport.LOGGER.info("[tour] spear charge: target health {} -> {}, husk at {}", health[0],
                        pig == null ? "dead" : pig.getHealth(), husk == null ? "?" : husk.position());
            });
        });
        // Player jab through the real attack key.
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            net.minecraft.world.entity.animal.Cow cow = net.minecraft.world.entity.EntityType.COW.create(level);
            cow.moveTo(base.getX() + 4.5, base.getY() + 1, base.getZ() + 3.5, 0, 0);
            cow.setNoAi(true);
            level.addFreshEntity(cow);
            ids[2] = cow.getId();
            health[1] = cow.getHealth();
            String name = m.player.getGameProfile().getName();
            exec(server, "gamemode survival " + name);
            exec(server, "effect give " + name + " minecraft:resistance infinite 255 true");
            exec(server, "item replace entity " + name + " weapon.mainhand with futurebackport:iron_spear");
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %.1f %d %.1f 0 10", name, base.getX() + 4.5, base.getY() + 1,
                    base.getZ() + 0.5));
        }));
        STEPS.add(m -> {
            m.options.hideGui = false;
            io.github.kestalf.futurebackport.client.SpearClient.jab(m.player, m.player.getMainHandItem());
        });
        STEPS.add(m -> pending = server.submit(() -> {
            var cow = (net.minecraft.world.entity.LivingEntity) server.overworld().getEntity(ids[2]);
            FutureBackport.LOGGER.info("[tour] spear jab: cow health {} -> {}", health[1], cow == null ? "dead" : cow.getHealth());
        }));
        STEPS.add(m -> shot(m, "spear_first_person"));
        STEPS.add(m -> {
            m.options.keyUse.setDown(true);
            m.gameMode.useItem(m.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        });
        STEPS.add(m -> shot(m, "spear_charge_first_person"));
        STEPS.add(m -> {
            m.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            pending = server.submit(() -> {
                var cow = server.overworld().getEntity(ids[2]);
                if (cow != null) cow.discard();
            });
        });
        STEPS.add(m -> shot(m, "spear_charge_third_person"));
        STEPS.add(m -> {
            m.options.keyUse.setDown(false);
            m.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            run(server, "gamemode creative " + m.player.getGameProfile().getName());
        });
    }

    /** Happy ghasts: an adult with a harness, a ghastling, then riding one (goggles down). */
    private static void addGhastScenario(MinecraftServer server) {
        BlockPos base = ORIGIN.offset(-80, 0, 0);
        int[] ids = new int[2];
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            exec(server, "gamemode spectator " + m.player.getGameProfile().getName());
            for (int x = -10; x <= 10; x++) {
                for (int z = -10; z <= 20; z++) {
                    level.setBlock(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 1; y < 12; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            var adult = io.github.kestalf.futurebackport.registry.ModEntities.HAPPY_GHAST.get().create(level);
            adult.moveTo(base.getX() - 3, base.getY() + 2, base.getZ(), 0, 0);
            adult.setBodyArmorItem(new net.minecraft.world.item.ItemStack(
                    io.github.kestalf.futurebackport.registry.ModItems.HARNESSES.get(net.minecraft.world.item.DyeColor.RED).get()));
            level.addFreshEntity(adult);
            var baby = io.github.kestalf.futurebackport.registry.ModEntities.HAPPY_GHAST.get().create(level);
            baby.setBaby(true);
            baby.moveTo(base.getX() + 4, base.getY() + 2, base.getZ() + 4, 0, 0);
            level.addFreshEntity(baby);
            ids[0] = adult.getId();
            ids[1] = baby.getId();
            level.setBlock(base.offset(4, 1, 8), io.github.kestalf.futurebackport.registry.ModBlocks.DRIED_GHAST.get().defaultBlockState(), 3);
        }));
        STEPS.add(m -> {
            m.options.hideGui = true;
            run(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 180 15", m.player.getGameProfile().getName(),
                    base.getX(), base.getY() + 5, base.getZ() + 14));
        });
        STEPS.add(m -> shot(m, "ghast_adult_and_baby"));
        STEPS.add(m -> pending = server.submit(() -> {
            exec(server, "gamemode creative " + m.player.getGameProfile().getName());
            var adult = server.overworld().getEntity(ids[0]);
            var player = server.getPlayerList().getPlayers().getFirst();
            if (adult != null) player.startRiding(adult, true);
            FutureBackport.LOGGER.info("[tour] ghast riding: vehicle={}", player.getVehicle());
        }));
        STEPS.add(m -> m.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        STEPS.add(m -> shot(m, "ghast_ridden"));
        STEPS.add(m -> {
            m.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            pending = server.submit(() -> server.getPlayerList().getPlayers().getFirst().stopRiding());
        });
    }

    /** The client ignores clicks until the attack key was released once after a screen closed; simulate that. */
    private static void resetMissTime(Minecraft mc) {
        try {
            var field = Minecraft.class.getDeclaredField("missTime");
            field.setAccessible(true);
            field.setInt(mc, 0);
        } catch (ReflectiveOperationException e) {
            FutureBackport.LOGGER.error("Could not reset missTime", e);
        }
    }

    private static void openCreativeTab(Minecraft mc) {
        CreativeModeInventoryScreen screen = new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true);
        mc.setScreen(screen);
        try {
            Method select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
            select.setAccessible(true);
            select.invoke(screen, ModCreativeTabs.MAIN.get());
        } catch (ReflectiveOperationException e) {
            FutureBackport.LOGGER.error("Could not select the creative tab", e);
        }
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "tour_" + name + ".png", mc.getMainRenderTarget(), message -> {
        });
        FutureBackport.LOGGER.info("[tour] screenshot {}", name);
    }

    /** Runs a command directly; only call this on the server thread. */
    private static void exec(MinecraftServer server, String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    private static void run(MinecraftServer server, String command) {
        pending = server.submit(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private ScreenshotTour() {
    }
}
