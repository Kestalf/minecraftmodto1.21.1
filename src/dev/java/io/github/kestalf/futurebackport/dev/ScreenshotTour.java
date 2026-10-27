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
    /** Ticks to wait after the current step; a step may shorten it. */
    private static int nextWait = 60;
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
        waitTicks = nextWait;
        nextWait = 60;
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
        if (scenario("creaking")) addCreakingScenario(server);
        if (scenario("golem")) addGolemScenario(server);
        if (scenario("shelf")) addShelfScenario(server);
        if (scenario("nautilus")) addNautilusScenario(server);
        if (scenario("desert")) addDesertScenario(server);
        if (scenario("farm")) addFarmScenario(server);
        if (scenario("sulfur")) addSulfurScenario(server);
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

    /**
     * Creaking checks: an awake heart spawns a creaking at night, looking at it freezes and activates it, hitting it
     * is blocked and grows resin, and breaking the heart makes it crumble.
     */
    private static void addCreakingScenario(MinecraftServer server) {
        BlockPos base = ORIGIN.offset(-130, 0, 0);
        BlockPos heartPos = base.above(2);
        java.util.UUID[] creakingId = new java.util.UUID[1];
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            String name = m.player.getGameProfile().getName();
            exec(server, "gamemode spectator " + name);
            for (int x = -18; x <= 18; x++) {
                for (int z = -18; z <= 18; z++) {
                    level.setBlock(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 1; y < 12; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            BlockState log = ModBlocks.PALE_OAK.log.get().defaultBlockState();
            level.setBlock(heartPos.below(), log, 3);
            level.setBlock(heartPos.above(), log, 3);
            level.setBlock(heartPos, ModBlocks.CREAKING_HEART.get().defaultBlockState(), 3);
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 180 10", name, heartPos.getX(), base.getY() + 1,
                    heartPos.getZ() + 4));
            exec(server, "gamemode survival " + name);
            exec(server, "effect give " + name + " minecraft:resistance infinite 255 true");
            exec(server, "time set 18000");
            exec(server, "gamerule doMobSpawning true");
        }));
        STEPS.add(m -> shot(m, "creaking_heart"));
        STEPS.add(m -> {
            // Look away so the creaking can walk.
            String name = m.player.getGameProfile().getName();
            run(server, "execute as " + name + " at @s run tp @s ~ ~ ~ 0 10");
        });
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            var creakings = level.getEntitiesOfClass(io.github.kestalf.futurebackport.entity.creaking.Creaking.class,
                    new net.minecraft.world.phys.AABB(base).inflate(24));
            FutureBackport.LOGGER.info("[tour] creaking heart state={} creakings={}", level.getBlockState(heartPos), creakings.size());
            if (level.getBlockEntity(heartPos) instanceof io.github.kestalf.futurebackport.block.entity.CreakingHeartBlockEntity heart) {
                heart.getCreakingProtector().ifPresentOrElse(c -> FutureBackport.LOGGER.info("[tour] heart protector {} removed={} alive={} pos={}",
                        c, c.getRemovalReason(), c.isAlive(), c.position()), () -> FutureBackport.LOGGER.info("[tour] heart has no protector"));
            }
            if (!creakings.isEmpty()) {
                var creaking = creakings.getFirst();
                creakingId[0] = creaking.getUUID();
                FutureBackport.LOGGER.info("[tour] creaking at {} home={} canMove={} active={}", creaking.position(), creaking.getHomePos(),
                        creaking.canMove(), creaking.isActive());
                exec(server, String.format(java.util.Locale.ROOT, "tp %s %.1f %.1f %.1f facing entity %s eyes",
                        m.player.getGameProfile().getName(), creaking.getX() + 3, creaking.getY(), creaking.getZ() + 3, creaking.getUUID()));
            }
        }));
        STEPS.add(m -> pending = server.submit(() -> {
            if (creakingId[0] == null) return;
            var creaking = (io.github.kestalf.futurebackport.entity.creaking.Creaking) server.overworld().getEntity(creakingId[0]);
            FutureBackport.LOGGER.info("[tour] looked at: canMove={} active={}", creaking.canMove(), creaking.isActive());
            // Stand in front of it for the screenshots.
            var front = creaking.position().add(net.minecraft.world.phys.Vec3.directionFromRotation(0, creaking.yBodyRot).scale(4));
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %.2f %.2f %.2f facing entity %s feet",
                    m.player.getGameProfile().getName(), front.x, creaking.getY() + 0.5, front.z, creaking.getUUID()));
        }));
        STEPS.add(m -> m.options.hideGui = true);
        STEPS.add(m -> shot(m, "creaking_watched"));
        STEPS.add(m -> pending = server.submit(() -> {
            if (creakingId[0] == null) return;
            ServerLevel level = server.overworld();
            var creaking = (io.github.kestalf.futurebackport.entity.creaking.Creaking) level.getEntity(creakingId[0]);
            var player = server.getPlayerList().getPlayers().getFirst();
            player.attack(creaking);
            int resin = 0;
            for (BlockPos pos : BlockPos.betweenClosed(heartPos.offset(-3, -3, -3), heartPos.offset(3, 3, 3))) {
                if (level.getBlockState(pos).is(ModBlocks.RESIN_CLUMP.get())) resin++;
            }
            FutureBackport.LOGGER.info("[tour] hit creaking: alive={} health={} resin clumps={}", creaking.isAlive(), creaking.getHealth(), resin);
        }));
        STEPS.add(m -> shot(m, "creaking_hit"));
        STEPS.add(m -> {
            nextWait = 15;
            pending = server.submit(() -> breakHeart(server, heartPos, creakingId[0]));
        });
        STEPS.add(m -> shot(m, "creaking_twitch"));
        STEPS.add(m -> pending = server.submit(() -> {
            if (creakingId[0] == null) return;
            FutureBackport.LOGGER.info("[tour] after heart broken: creaking present={}", server.overworld().getEntity(creakingId[0]) != null);
            exec(server, "gamerule doMobSpawning false");
            exec(server, "time set 6000");
        }));
    }

    /**
     * Copper golem checks: building one from a pumpkin, carrying items from its copper chest to a wooden chest,
     * copper chests keeping their items when they oxidize, statue poses and reanimating a statue with an axe.
     */
    private static void addGolemScenario(MinecraftServer server) {
        BlockPos base = ORIGIN.offset(-170, 0, 0);
        java.util.UUID[] golemId = new java.util.UUID[1];
        var copper = ModBlocks.COPPER;
        var stages = net.minecraft.world.level.block.WeatheringCopper.WeatherState.values();
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            String name = m.player.getGameProfile().getName();
            exec(server, "gamemode spectator " + name);
            exec(server, "time set 6000");
            for (int x = -10; x <= 10; x++) {
                for (int z = -10; z <= 10; z++) {
                    level.setBlock(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 1; y < 8; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            // Build a golem: copper block, then a carved pumpkin on top.
            level.setBlock(base.above(), Blocks.COPPER_BLOCK.defaultBlockState(), 3);
            level.setBlock(base.above(2), Blocks.CARVED_PUMPKIN.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
            var golems = level.getEntitiesOfClass(io.github.kestalf.futurebackport.entity.golem.CopperGolem.class, new net.minecraft.world.phys.AABB(base).inflate(4));
            FutureBackport.LOGGER.info("[tour] golem built: golems={} block below={} pumpkin pos={}", golems.size(), level.getBlockState(base.above()),
                    level.getBlockState(base.above(2)));
            if (!golems.isEmpty()) golemId[0] = golems.getFirst().getUUID();
            if (level.getBlockEntity(base.above()) instanceof net.minecraft.world.Container chest) {
                chest.setItem(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 20));
                chest.setItem(5, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
            }
            level.setBlock(base.offset(5, 1, 0), Blocks.CHEST.defaultBlockState(), 3);
            // Copper chests of all stages (and a double one) and statues in all poses.
            for (int i = 0; i < 4; i++) {
                level.setBlock(base.offset(-6 + i * 2, 1, 6), copper.chests.get(stages[i], false).defaultBlockState(), 3);
                level.setBlock(base.offset(-6 + i * 2, 1, 8), copper.statues.get(stages[i], i % 2 == 1).defaultBlockState()
                        .setValue(io.github.kestalf.futurebackport.block.CopperGolemStatueBlock.FACING, net.minecraft.core.Direction.SOUTH)
                        .setValue(io.github.kestalf.futurebackport.block.CopperGolemStatueBlock.POSE,
                                io.github.kestalf.futurebackport.block.CopperGolemPose.values()[i]), 3);
            }
            var west = copper.chests.get(stages[0], false).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.ChestBlock.FACING, net.minecraft.core.Direction.SOUTH);
            level.setBlock(base.offset(3, 1, 6), west.setValue(net.minecraft.world.level.block.ChestBlock.TYPE,
                    net.minecraft.world.level.block.state.properties.ChestType.RIGHT), 3);
            level.setBlock(base.offset(4, 1, 6), copper.chests.get(stages[2], false).defaultBlockState()
                    .setValue(net.minecraft.world.level.block.ChestBlock.FACING, net.minecraft.core.Direction.SOUTH)
                    .setValue(net.minecraft.world.level.block.ChestBlock.TYPE, net.minecraft.world.level.block.state.properties.ChestType.LEFT), 3);
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 0 25", name, base.getX(), base.getY() + 4, base.getZ() - 6));
        }));
        STEPS.add(m -> m.options.hideGui = true);
        STEPS.add(m -> shot(m, "golem_built"));
        STEPS.add(m -> {});
        STEPS.add(m -> {});
        STEPS.add(m -> pending = server.submit(() -> logGolem(server, golemId[0], base)));
        STEPS.add(m -> shot(m, "golem_working"));
        STEPS.add(m -> {});
        STEPS.add(m -> {});
        STEPS.add(m -> pending = server.submit(() -> logGolem(server, golemId[0], base)));
        STEPS.add(m -> pending = server.submit(() -> {
            // Close-up of the golem, frozen and facing the camera, holding an item.
            var golem = (io.github.kestalf.futurebackport.entity.golem.CopperGolem) server.overworld().getEntity(golemId[0]);
            if (golem == null) return;
            golem.setNoAi(true);
            golem.moveTo(base.getX() + 0.5, base.getY() + 1, base.getZ() - 2.5, 180.0F, 0.0F);
            golem.setYHeadRot(180.0F);
            golem.setYBodyRot(180.0F);
            golem.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %.1f %.1f %.1f 0 20", m.player.getGameProfile().getName(),
                    base.getX() + 0.5, base.getY() + 1.6, base.getZ() - 5.0));
        }));
        STEPS.add(m -> shot(m, "golem_closeup"));
        STEPS.add(m -> run(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 180 30", m.player.getGameProfile().getName(),
                base.getX() - 3, base.getY() + 3, base.getZ() + 12)));
        STEPS.add(m -> shot(m, "copper_chests_and_statues"));
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            // Oxidize a chest holding an item: the item must survive the block change.
            BlockPos chestPos = base.offset(-6, 1, 6);
            if (level.getBlockEntity(chestPos) instanceof net.minecraft.world.Container chest) {
                chest.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.APPLE, 3));
            }
            var current = level.getBlockState(chestPos);
            net.minecraft.world.level.block.WeatheringCopper.getNext(current.getBlock())
                    .ifPresent(block -> level.setBlockAndUpdate(chestPos, block.withPropertiesOf(current)));
            FutureBackport.LOGGER.info("[tour] oxidized chest: {} item={}", level.getBlockState(chestPos).getBlock(),
                    level.getBlockEntity(chestPos) instanceof net.minecraft.world.Container c ? c.getItem(0) : "no container");
            // Reanimate the unaffected statue with an axe.
            BlockPos statuePos = base.offset(-6, 1, 8);
            var player = server.getPlayerList().getPlayers().getFirst();
            var axe = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, axe);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(statuePos), net.minecraft.core.Direction.UP,
                    statuePos, false);
            var result = level.getBlockState(statuePos).useItemOn(axe, level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            var golems = level.getEntitiesOfClass(io.github.kestalf.futurebackport.entity.golem.CopperGolem.class,
                    new net.minecraft.world.phys.AABB(statuePos).inflate(1));
            FutureBackport.LOGGER.info("[tour] statue reanimation: result={} block={} golems={}", result, level.getBlockState(statuePos), golems.size());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        }));
        STEPS.add(m -> shot(m, "statue_reanimated"));
    }

    /** Shelf checks: slot swapping, comparator output, powered chains of three and swapping a chain with the hotbar. */
    private static void addShelfScenario(MinecraftServer server) {
        BlockPos base = ORIGIN.offset(-210, 0, 0);
        var south = net.minecraft.core.Direction.SOUTH;
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            String name = m.player.getGameProfile().getName();
            exec(server, "gamemode survival " + name);
            for (int x = -8; x <= 8; x++) {
                for (int z = -6; z <= 8; z++) {
                    level.setBlock(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 1; y < 6; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            var shelves = new java.util.ArrayList<>(ModBlocks.SHELVES.values());
            // A single unpowered shelf filled through the real interaction, plus every wood type for the textures.
            for (int i = 0; i < shelves.size(); i++) {
                level.setBlock(base.offset(-6 + i, 2, 0), shelves.get(i).get().defaultBlockState()
                        .setValue(io.github.kestalf.futurebackport.block.ShelfBlock.FACING, south), 3);
            }
            var player = server.getPlayerList().getPlayers().getFirst();
            BlockPos single = base.offset(-6, 2, 0);
            var items = new net.minecraft.world.item.Item[]{net.minecraft.world.item.Items.DIAMOND, net.minecraft.world.item.Items.COBBLESTONE,
                    net.minecraft.world.item.Items.APPLE};
            for (int slot = 0; slot < 3; slot++) {
                // South-facing: the viewer's left is west (-x).
                double x = single.getX() + 0.5 + (slot - 1) * 0.33;
                var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(x, single.getY() + 0.5, single.getZ() + 0.9),
                        south, single, false);
                var stack = new net.minecraft.world.item.ItemStack(items[slot], 5);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                level.getBlockState(single).useItemOn(stack, level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            }
            var shelf = (io.github.kestalf.futurebackport.block.entity.ShelfBlockEntity) level.getBlockEntity(single);
            FutureBackport.LOGGER.info("[tour] shelf slots: {} {} {} comparator={} hand={}", shelf.getItem(0), shelf.getItem(1), shelf.getItem(2),
                    level.getBlockState(single).getAnalogOutputSignal(level, single), player.getMainHandItem());
            // Three powered shelves join into one chain.
            for (int i = 0; i < 3; i++) {
                BlockPos pos = base.offset(-1 + i, 1, 4);
                level.setBlock(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                level.setBlock(pos, shelves.get(0).get().defaultBlockState().setValue(io.github.kestalf.futurebackport.block.ShelfBlock.FACING, south), 3);
            }
            FutureBackport.LOGGER.info("[tour] powered row: {} | {} | {}", level.getBlockState(base.offset(-1, 1, 4)), level.getBlockState(base.offset(0, 1, 4)),
                    level.getBlockState(base.offset(1, 1, 4)));
            for (int i = 0; i < 9; i++) {
                player.getInventory().setItem(i, new net.minecraft.world.item.ItemStack(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.byId(20 + i * 7), i + 1));
            }
            String before = player.getInventory().items.subList(0, 9).toString();
            BlockPos middle = base.offset(0, 1, 4);
            var hit = new net.minecraft.world.phys.BlockHitResult(new net.minecraft.world.phys.Vec3(middle.getX() + 0.5, middle.getY() + 0.5,
                    middle.getZ() + 0.9), south, middle, false);
            level.getBlockState(middle).useItemOn(player.getMainHandItem(), level, player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            FutureBackport.LOGGER.info("[tour] hotbar before={} after={}", before, player.getInventory().items.subList(0, 9));
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %d %.1f %d 180 15", name, base.getX(), base.getY() + 2.5, base.getZ() + 7));
        }));
        STEPS.add(m -> m.options.hideGui = true);
        STEPS.add(m -> shot(m, "shelves"));
        STEPS.add(m -> run(server, String.format(java.util.Locale.ROOT, "tp %s %.1f %.1f %.1f 180 10", m.player.getGameProfile().getName(),
                base.getX() - 5.5, base.getY() + 2.6, base.getZ() + 2.2)));
        STEPS.add(m -> shot(m, "shelf_closeup"));
    }

    /** Nautilus checks: a tamed, saddled and armored nautilus, a baby, a coral zombie nautilus, drowned jockeys, riding. */
    private static void addNautilusScenario(MinecraftServer server) {
        BlockPos base = ORIGIN.offset(-250, 0, 0);
        int[] ids = new int[4];
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            String name = m.player.getGameProfile().getName();
            exec(server, "gamemode spectator " + name);
            for (int x = -9; x <= 9; x++) {
                for (int z = -9; z <= 9; z++) {
                    for (int y = 0; y <= 7; y++) {
                        boolean wall = Math.abs(x) == 9 || Math.abs(z) == 9 || y == 0;
                        level.setBlock(base.offset(x, y, z), wall ? Blocks.GLASS.defaultBlockState()
                                : y == 7 ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState(), 2);
                    }
                }
            }
            var player = server.getPlayerList().getPlayers().getFirst();
            var adult = io.github.kestalf.futurebackport.registry.ModEntities.NAUTILUS.get().create(level);
            adult.moveTo(base.getX() - 3, base.getY() + 3, base.getZ() + 2, 180, 0);
            adult.tame(player);
            adult.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), null);
            adult.setBodyArmorItem(new net.minecraft.world.item.ItemStack(io.github.kestalf.futurebackport.registry.ModItems.DIAMOND_NAUTILUS_ARMOR.get()));
            adult.setNoAi(true);
            level.addFreshEntity(adult);
            var baby = io.github.kestalf.futurebackport.registry.ModEntities.NAUTILUS.get().create(level);
            baby.moveTo(base.getX(), base.getY() + 3, base.getZ() + 2, 180, 0);
            baby.setAge(-24000);
            baby.setNoAi(true);
            level.addFreshEntity(baby);
            var zombie = io.github.kestalf.futurebackport.registry.ModEntities.ZOMBIE_NAUTILUS.get().create(level);
            zombie.moveTo(base.getX() + 3, base.getY() + 3, base.getZ() + 2, 180, 0);
            zombie.readAdditionalSaveData(new net.minecraft.nbt.CompoundTag() {{ putBoolean("coral", true); }});
            zombie.setNoAi(true);
            level.addFreshEntity(zombie);
            ids[0] = adult.getId();
            // Drowned with tridents: about half of them should come riding a zombie nautilus.
            int jockeys = 0;
            for (int i = 0; i < 20; i++) {
                var drowned = net.minecraft.world.entity.EntityType.DROWNED.create(level);
                drowned.moveTo(base.getX() + 6, base.getY() + 2, base.getZ() - 6, 0, 0);
                drowned.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TRIDENT));
                drowned.finalizeSpawn(level, level.getCurrentDifficultyAt(drowned.blockPosition()), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
                if (drowned.getVehicle() instanceof io.github.kestalf.futurebackport.entity.nautilus.ZombieNautilus mount) {
                    jockeys++;
                    if (jockeys == 1 && drowned.getMainHandItem().is(net.minecraft.world.item.Items.TRIDENT)) {
                        level.addFreshEntity(drowned);
                        drowned.setNoAi(true);
                        mount.setNoAi(true);
                        continue;
                    }
                    mount.discard();
                }
                drowned.discard();
            }
            FutureBackport.LOGGER.info("[tour] nautilus: drowned jockeys {}/20", jockeys);
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 0 20", name, base.getX(), base.getY() + 5, base.getZ() - 12));
        }));
        STEPS.add(m -> m.options.hideGui = true);
        STEPS.add(m -> shot(m, "nautiluses"));
        STEPS.add(m -> pending = server.submit(() -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            var adult = (io.github.kestalf.futurebackport.entity.nautilus.AbstractNautilus) server.overworld().getEntity(ids[0]);
            exec(server, "gamemode creative " + player.getGameProfile().getName());
            player.teleportTo(adult.getX(), adult.getY() + 1, adult.getZ());
            FutureBackport.LOGGER.info("[tour] mounting: {}", player.startRiding(adult, true));
        }));
        STEPS.add(m -> pending = server.submit(() -> {
            var player = server.getPlayerList().getPlayers().getFirst();
            var adult = (io.github.kestalf.futurebackport.entity.nautilus.AbstractNautilus) server.overworld().getEntity(ids[0]);
            FutureBackport.LOGGER.info("[tour] riding nautilus: vehicle={} breath effect={} armor={} canJump={}", player.getVehicle(),
                    player.hasEffect(io.github.kestalf.futurebackport.registry.ModEffects.BREATH_OF_THE_NAUTILUS), adult.getArmorValue(), adult.canJump());
        }));
        STEPS.add(m -> {
            m.options.hideGui = false;
            m.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
        });
        STEPS.add(m -> shot(m, "nautilus_ridden"));
        STEPS.add(m -> {
            m.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            pending = server.submit(() -> server.getPlayerList().getPlayers().getFirst().stopRiding());
        });
    }

    /** Camel husk jockeys (husk with an iron spear and a parched riding a camel husk), a saddled camel husk, a parched. */
    private static void addDesertScenario(MinecraftServer server) {
        BlockPos base = ORIGIN.offset(-290, 0, 0);
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            String name = m.player.getGameProfile().getName();
            exec(server, "gamemode spectator " + name);
            for (int x = -12; x <= 12; x++) {
                for (int z = -6; z <= 12; z++) {
                    level.setBlock(base.offset(x, 0, z), Blocks.SANDSTONE.defaultBlockState(), 2);
                    for (int y = 1; y < 8; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            int jockeys = 0;
            for (int i = 0; i < 60; i++) {
                var husk = net.minecraft.world.entity.EntityType.HUSK.create(level);
                husk.moveTo(base.getX() - 4, base.getY() + 1, base.getZ() + 4, 180, 0);
                husk.finalizeSpawn(level, level.getCurrentDifficultyAt(husk.blockPosition()), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
                if (husk.getVehicle() instanceof io.github.kestalf.futurebackport.entity.desert.CamelHusk camel) {
                    jockeys++;
                    if (jockeys == 1) {
                        FutureBackport.LOGGER.info("[tour] camel husk jockey: passengers={} husk weapon={}", camel.getPassengers(), husk.getMainHandItem());
                        level.addFreshEntity(husk);
                        camel.setNoAi(true);
                        husk.setNoAi(true);
                        camel.getPassengers().forEach(p -> { if (p instanceof net.minecraft.world.entity.Mob mob) mob.setNoAi(true); });
                        continue;
                    }
                    camel.getPassengers().forEach(net.minecraft.world.entity.Entity::discard);
                    camel.discard();
                }
                husk.discard();
            }
            FutureBackport.LOGGER.info("[tour] camel husk jockeys {}/60", jockeys);
            var saddled = io.github.kestalf.futurebackport.registry.ModEntities.CAMEL_HUSK.get().create(level);
            saddled.moveTo(base.getX() + 3, base.getY() + 1, base.getZ() + 4, 180, 0);
            saddled.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), null);
            saddled.setNoAi(true);
            level.addFreshEntity(saddled);
            var parched = io.github.kestalf.futurebackport.registry.ModEntities.PARCHED.get().create(level);
            parched.moveTo(base.getX() + 7, base.getY() + 1, base.getZ() + 3, 180, 0);
            parched.finalizeSpawn(level, level.getCurrentDifficultyAt(parched.blockPosition()), net.minecraft.world.entity.MobSpawnType.NATURAL, null);
            parched.setNoAi(true);
            level.addFreshEntity(parched);
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 0 15", name, base.getX(), base.getY() + 4, base.getZ() - 5));
        }));
        STEPS.add(m -> m.options.hideGui = true);
        STEPS.add(m -> shot(m, "desert_mobs"));
    }

    /** Warm and cold chickens, cows and pigs, coloured eggs and cushions. */
    private static void addFarmScenario(MinecraftServer server) {
        BlockPos base = ORIGIN.offset(-330, 0, 0);
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            String name = m.player.getGameProfile().getName();
            exec(server, "gamemode spectator " + name);
            for (int x = -10; x <= 10; x++) {
                for (int z = -6; z <= 10; z++) {
                    level.setBlock(base.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                    for (int y = 1; y < 8; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            var variants = io.github.kestalf.futurebackport.entity.variant.FarmAnimalVariant.values();
            var types = List.<net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.animal.Animal>>of(
                    net.minecraft.world.entity.EntityType.CHICKEN, net.minecraft.world.entity.EntityType.COW, net.minecraft.world.entity.EntityType.PIG);
            for (int t = 0; t < types.size(); t++) {
                for (int v = 0; v < variants.length; v++) {
                    for (boolean baby : new boolean[]{false, true}) {
                        var animal = types.get(t).create(level);
                        animal.setData(io.github.kestalf.futurebackport.registry.ModAttachments.FARM_VARIANT, variants[v]);
                        animal.moveTo(base.getX() - 6 + v * 5 + (baby ? 2 : 0), base.getY() + 1, base.getZ() + t * 3, 180, 0);
                        if (baby) animal.setAge(-24000);
                        animal.setNoAi(true);
                        level.addFreshEntity(animal);
                    }
                }
            }
            // Laying and hatching.
            var cold = net.minecraft.world.entity.EntityType.CHICKEN.create(level);
            cold.setData(io.github.kestalf.futurebackport.registry.ModAttachments.FARM_VARIANT, variants[2]);
            cold.moveTo(base.getX() + 8, base.getY() + 1, base.getZ() - 4, 0, 0);
            level.addFreshEntity(cold);
            cold.eggTime = 1;
            cold.aiStep();
            var eggs = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, cold.getBoundingBox().inflate(2));
            io.github.kestalf.futurebackport.entity.variant.FarmVariantEvents.startHatching(
                    new net.minecraft.world.item.ItemStack(io.github.kestalf.futurebackport.registry.ModItems.BROWN_EGG.get()));
            var chick = net.minecraft.world.entity.EntityType.CHICKEN.create(level);
            chick.moveTo(base.getX() + 8, base.getY() + 1, base.getZ() - 2, 0, 0);
            level.addFreshEntity(chick);
            io.github.kestalf.futurebackport.entity.variant.FarmVariantEvents.stopHatching();
            var natural = net.minecraft.world.entity.EntityType.PIG.create(level);
            natural.moveTo(base.getX() + 8, base.getY() + 1, base.getZ(), 0, 0);
            level.addFreshEntity(natural);
            FutureBackport.LOGGER.info("[tour] farm: cold chicken laid {} | brown egg chick {} | pig here {} (biome {})",
                    eggs.stream().map(e -> e.getItem().toString()).toList(),
                    io.github.kestalf.futurebackport.entity.variant.FarmAnimalVariant.of(chick),
                    io.github.kestalf.futurebackport.entity.variant.FarmAnimalVariant.of(natural), level.getBiome(natural.blockPosition()).getRegisteredName());
            cold.discard();
            chick.discard();
            natural.discard();
            // Cushions placed through the item, one on a slab.
            var player = server.getPlayerList().getPlayers().getFirst();
            exec(server, "gamemode creative " + name);
            level.setBlock(base.offset(-6, 1, 8), Blocks.OAK_SLAB.defaultBlockState(), 3);
            int placed = 0;
            for (int i = 0; i < 4; i++) {
                BlockPos ground = base.offset(-6 + i * 2, i == 0 ? 1 : 0, 8);
                var stack = new net.minecraft.world.item.ItemStack(io.github.kestalf.futurebackport.registry.ModItems.CUSHIONS.get(
                        net.minecraft.world.item.DyeColor.values()[i * 4]).get());
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(ground).add(0, 0.5, 0),
                        net.minecraft.core.Direction.UP, ground, false);
                var result = stack.useOn(new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
                if (result.consumesAction()) placed++;
            }
            var cushions = level.getEntitiesOfClass(io.github.kestalf.futurebackport.entity.Cushion.class, new net.minecraft.world.phys.AABB(base).inflate(12));
            FutureBackport.LOGGER.info("[tour] cushions placed {} -> {}", placed, cushions.stream().map(c -> c.getColor() + "@" + c.getY()).toList());
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            exec(server, "gamemode spectator " + name);
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 0 25", name, base.getX(), base.getY() + 5, base.getZ() - 7));
        }));
        STEPS.add(m -> m.options.hideGui = true);
        STEPS.add(m -> shot(m, "farm_variants"));
    }

    private static void addSulfurScenario(MinecraftServer server) {
        BlockPos base = ORIGIN.offset(-380, 0, 0);
        java.util.UUID[] ids = new java.util.UUID[3];
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            String name = m.player.getGameProfile().getName();
            exec(server, "gamemode spectator " + name);
            exec(server, "kill @e[type=item]");
            for (int x = -14; x <= 14; x++) {
                for (int z = -8; z <= 12; z++) {
                    level.setBlock(base.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                    level.setBlock(base.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 2);
                    for (int y = 1; y < 12; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
            // Three pools, 3 blocks of water over potent sulfur: plain (gas), over magma (geyser), over lava (continuous).
            Block[] under = {Blocks.STONE, Blocks.MAGMA_BLOCK, Blocks.LAVA};
            for (int i = 0; i < 3; i++) {
                BlockPos centre = base.offset(-8 + i * 8, 0, 0);
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        boolean wall = Math.abs(dx) == 3 || Math.abs(dz) == 3;
                        level.setBlock(centre.offset(dx, 1, dz), wall ? Blocks.GLASS.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                        for (int y = 2; y <= 4; y++) {
                            level.setBlock(centre.offset(dx, y, dz), wall ? Blocks.GLASS.defaultBlockState() : Blocks.WATER.defaultBlockState(), 2);
                        }
                    }
                }
                level.setBlock(centre, under[i].defaultBlockState(), 2);
                BlockPos sulfur = centre.above();
                level.setBlock(sulfur, io.github.kestalf.futurebackport.block.PotentSulfurBlock.withEnvironment(
                        io.github.kestalf.futurebackport.registry.ModBlocks.POTENT_SULFUR.get().defaultBlockState(), level, sulfur), 3);
                var pig = net.minecraft.world.entity.EntityType.PIG.create(level);
                pig.moveTo(sulfur.getX() + 0.5, sulfur.getY() + 2, sulfur.getZ() + 0.5, 0, 0);
                pig.setPersistenceRequired();
                level.addFreshEntity(pig);
                ids[i] = pig.getUUID();
            }
            // Make the magma geyser erupt now instead of in 35-50 seconds.
            BlockPos magma = base.offset(0, 1, 0);
            level.setBlock(magma, level.getBlockState(magma).setValue(io.github.kestalf.futurebackport.block.PotentSulfurBlock.STATE,
                    io.github.kestalf.futurebackport.block.PotentSulfurBlock.State.ERUPTING), 3);
            FutureBackport.LOGGER.info("[tour] sulfur states: {} {} {} | depth {}",
                    level.getBlockState(base.offset(-8, 1, 0)), level.getBlockState(magma), level.getBlockState(base.offset(8, 1, 0)),
                    io.github.kestalf.futurebackport.block.entity.PotentSulfurBlockEntity.waterDepth(level, magma));

            // Sulfur spikes: a stalactite under sulfur that grows, and one that falls when its support goes.
            var spike = io.github.kestalf.futurebackport.registry.ModBlocks.SULFUR_SPIKE.get();
            var down = spike.defaultBlockState().setValue(io.github.kestalf.futurebackport.block.SulfurSpikeBlock.TIP_DIRECTION, net.minecraft.core.Direction.DOWN);
            for (int i = 0; i < 2; i++) {
                BlockPos ceiling = base.offset(-4 + i * 8, 9, 8);
                level.setBlock(ceiling, io.github.kestalf.futurebackport.registry.ModBlocks.SULFUR.base().get().defaultBlockState(), 3);
                level.setBlock(ceiling.below(), down, 3);
            }
            BlockPos growing = base.offset(-4, 8, 8);
            var random = net.minecraft.util.RandomSource.create(1);
            for (int t = 0; t < 20000; t++) {
                BlockPos tip = growing;
                while (level.getBlockState(tip.below()).is(spike)) tip = tip.below();
                var state = level.getBlockState(tip);
                if (state.isRandomlyTicking()) state.randomTick(level, tip, random);
            }
            StringBuilder column = new StringBuilder();
            for (int y = 9; y >= 0; y--) {
                var state = level.getBlockState(base.offset(-4, y, 8));
                column.append(y).append('=').append(state.is(spike) ? state.getValue(io.github.kestalf.futurebackport.block.SulfurSpikeBlock.TIP_DIRECTION)
                        + "/" + state.getValue(io.github.kestalf.futurebackport.block.SulfurSpikeBlock.THICKNESS) : state.getBlock().getName().getString()).append(' ');
            }
            FutureBackport.LOGGER.info("[tour] spike column after growth: {}", column);
            level.setBlock(base.offset(4, 9, 8), Blocks.AIR.defaultBlockState(), 3);

            // Straw beds: one to sleep in (at night, set a step ahead so that the sky darkens) and one for the screenshots.
            exec(server, "time set 18000");
            var bed = io.github.kestalf.futurebackport.registry.ModBlocks.STRAW_BED.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.BedBlock.FACING, net.minecraft.core.Direction.SOUTH);
            for (BlockPos foot : List.of(base.offset(10, 1, 8), base.offset(12, 1, 8))) {
                level.setBlock(foot, bed.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT), 3);
                level.setBlock(foot.south(), bed.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD), 3);
            }
            var player = server.getPlayerList().getPlayers().getFirst();
            player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(io.github.kestalf.futurebackport.registry.ModBlocks.STRAW_BED.get()));
            player.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(io.github.kestalf.futurebackport.registry.ModBlocks.POTENT_SULFUR.get()));
            player.getInventory().setItem(2, new net.minecraft.world.item.ItemStack(spike));
        }));
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            var player = server.getPlayerList().getPlayers().getFirst();
            exec(server, "gamemode survival " + player.getGameProfile().getName());
            BlockPos head = base.offset(10, 1, 9);
            player.teleportTo(level, head.getX() + 0.5, head.getY(), head.getZ() + 1.5, 180, 0);
            var before = player.getRespawnPosition();
            var sleep = player.startSleepInBed(head);
            FutureBackport.LOGGER.info("[tour] straw bed: problem={} sleeping={} occupied={} respawn before={} after={}", sleep.left(),
                    player.isSleeping(), level.getBlockState(head).getValue(net.minecraft.world.level.block.BedBlock.OCCUPIED), before,
                    player.getRespawnPosition());
            player.stopSleepInBed(false, true);
        }));
        STEPS.add(m -> pending = server.submit(() -> {
            ServerLevel level = server.overworld();
            BlockPos head = base.offset(10, 1, 9);
            java.util.function.Function<net.minecraft.world.phys.AABB, List<String>> drops = box -> level.getEntitiesOfClass(
                    net.minecraft.world.entity.item.ItemEntity.class, box).stream().map(e -> e.getItem() + "@" + e.blockPosition().subtract(base)
                    + " age " + e.getAge()).toList();
            FutureBackport.LOGGER.info("[tour] straw bed after getting up: head={} foot={} bed drops={} | spike drops={}",
                    level.getBlockState(head).getBlock().getName().getString(), level.getBlockState(head.north()).getBlock().getName().getString(),
                    drops.apply(new net.minecraft.world.phys.AABB(head).inflate(3)), drops.apply(new net.minecraft.world.phys.AABB(base.offset(4, 1, 8)).inflate(2)));
            for (int i = 0; i < 3; i++) {
                var pig = (net.minecraft.world.entity.LivingEntity) level.getEntity(ids[i]);
                FutureBackport.LOGGER.info("[tour] pool {}: state={} pig y={} nausea={}", i,
                        level.getBlockState(base.offset(-8 + i * 8, 1, 0)).getValue(io.github.kestalf.futurebackport.block.PotentSulfurBlock.STATE),
                        pig == null ? "gone" : String.format(java.util.Locale.ROOT, "%.2f", pig.getY() - base.getY()),
                        pig != null && pig.hasEffect(net.minecraft.world.effect.MobEffects.CONFUSION));
            }
            String name = m.player.getGameProfile().getName();
            exec(server, "time set 6000");
            exec(server, "gamemode spectator " + name);
            exec(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 0 30", name, base.getX(), base.getY() + 9, base.getZ() - 14));
        }));
        STEPS.add(m -> m.options.hideGui = true);
        STEPS.add(m -> shot(m, "sulfur_pools"));
        STEPS.add(m -> run(server, String.format(java.util.Locale.ROOT, "tp %s %d %d %d 0 12", m.player.getGameProfile().getName(),
                base.getX() + 3, base.getY() + 4, base.getZ() + 3)));
        STEPS.add(m -> shot(m, "sulfur_spikes_beds"));
        STEPS.add(m -> {
            m.options.hideGui = false;
            run(server, "gamemode creative " + m.player.getGameProfile().getName());
        });
        STEPS.add(m -> shot(m, "sulfur_items"));
    }

    private static void logGolem(MinecraftServer server, java.util.UUID golemId, BlockPos base) {
        ServerLevel level = server.overworld();
        var golem = golemId == null ? null : (io.github.kestalf.futurebackport.entity.golem.CopperGolem) level.getEntity(golemId);
        String target = level.getBlockEntity(base.offset(5, 1, 0)) instanceof net.minecraft.world.Container c
                ? java.util.stream.IntStream.range(0, c.getContainerSize()).mapToObj(c::getItem).filter(st -> !st.isEmpty()).toList().toString() : "?";
        String source = level.getBlockEntity(base.above()) instanceof net.minecraft.world.Container c
                ? java.util.stream.IntStream.range(0, c.getContainerSize()).mapToObj(c::getItem).filter(st -> !st.isEmpty()).toList().toString() : "?";
        FutureBackport.LOGGER.info("[tour] golem: pos={} held={} interaction={} | copper chest={} | wooden chest={}",
                golem == null ? "gone" : golem.position(), golem == null ? "-" : golem.getMainHandItem(),
                golem == null ? "-" : golem.getInteraction(), source, target);
    }

    private static void breakHeart(MinecraftServer server, BlockPos heartPos, java.util.UUID creakingId) {
        if (creakingId == null) return;
        var player = server.getPlayerList().getPlayers().getFirst();
        exec(server, "gamemode creative " + player.getGameProfile().getName());
        player.gameMode.destroyBlock(heartPos);
        var creaking = (io.github.kestalf.futurebackport.entity.creaking.Creaking) server.overworld().getEntity(creakingId);
        FutureBackport.LOGGER.info("[tour] heart broken: tearingDown={} health={}", creaking == null ? "gone" : creaking.isTearingDown(),
                creaking == null ? 0 : creaking.getHealth());
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
