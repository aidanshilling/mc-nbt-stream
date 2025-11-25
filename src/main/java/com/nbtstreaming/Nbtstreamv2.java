package com.nbtstreaming;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static net.minecraft.server.command.CommandManager.literal;

public class Nbtstreamv2 implements ModInitializer {
    public static final String MOD_ID = "nbt-stream-v2";
    private static final double NEARBY_RADIUS = 20.0;
    private static final Set<UUID> POSITION_SUBSCRIBERS = new HashSet<>();
    private static final int WEBSOCKET_PORT = 8765;
    private static PositionStreamSocketServer socketServer;

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        startWebSocketServer();
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> stopWebSocketServer());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<UUID> iterator = POSITION_SUBSCRIBERS.iterator();
            while (iterator.hasNext()) {
                UUID uuid = iterator.next();
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);

                if (player == null) {
                    iterator.remove();
                    continue;
                }

                player.sendMessage(
                        Text.literal("Your position: " + player.getX() + ", " + player.getY() + ", " + player.getZ()),
                        false
                );

                ServerWorld world = player.getEntityWorld();
                List<Entity> nearbyEntities = new ArrayList<>(world.getOtherEntities(
                        player,
                        player.getBoundingBox().expand(NEARBY_RADIUS)
                ));

                String names = nearbyEntities.stream()
                        .map(entity -> entity.getName().getString())
                        .sorted()
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("No entities nearby.");

                player.sendMessage(Text.literal("Entities within " + NEARBY_RADIUS + " blocks: " + names), false);

                String payload = buildStreamJson(world, player, nearbyEntities);
                LOGGER.info(payload);
                sendOverWebSocket(payload);
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            dispatcher.register(
                    literal("pos")
                            .executes(ctx -> {
                                PlayerEntity player = ctx.getSource().getPlayer();

                                assert player != null;

                                POSITION_SUBSCRIBERS.add(player.getUuid());

                                ctx.getSource().sendFeedback(
                                        () -> Text.literal("Now sending your position every tick."),
                                        false
                                );

                                return 1;
                            })
                            .then(literal("stop").executes(ctx -> {
                                PlayerEntity player = ctx.getSource().getPlayer();

                                assert player != null;

                                boolean wasSubscribed = POSITION_SUBSCRIBERS.remove(player.getUuid());
                                Text response = wasSubscribed
                                        ? Text.literal("Stopped sending your position.")
                                        : Text.literal("You do not have an active position stream.");

                                ctx.getSource().sendFeedback(() -> response, false);

                                return wasSubscribed ? 1 : 0;
                            }))
            );
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("test_command").executes(context -> {
                context.getSource().sendFeedback(() -> Text.literal("Called /test_command."), false);
                return 1;
            }));
        });

        LOGGER.info("Hello Fabric world!");
    }

    private static void startWebSocketServer() {
        socketServer = new PositionStreamSocketServer(WEBSOCKET_PORT);
        try {
            socketServer.start();
        } catch (Exception ex) {
            LOGGER.error("Failed to start WebSocket server on port {}", WEBSOCKET_PORT, ex);
            socketServer = null;
        }
    }

    private static void stopWebSocketServer() {
        if (socketServer == null) {
            return;
        }

        try {
            socketServer.stop();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            socketServer = null;
        }
    }

    private static void sendOverWebSocket(String payload) {
        if (socketServer != null && !socketServer.getConnections().isEmpty()) {
            socketServer.broadcastPayload(payload);
        }
    }

    private static String buildStreamJson(ServerWorld world, ServerPlayerEntity player, List<Entity> nearbyEntities) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"player\":");
        appendEntitySection(sb, world, player);
        sb.append(",\"entities\":[");
        for (int i = 0; i < nearbyEntities.size(); i++) {
            appendEntitySection(sb, world, nearbyEntities.get(i));
            if (i < nearbyEntities.size() - 1) {
                sb.append(',');
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    private static void appendEntitySection(StringBuilder sb, ServerWorld world, Entity entity) {
        BlockPos blockPos = entity.getBlockPos().down();
        sb.append("{\"name\":\"").append(jsonEscape(entity.getName().getString())).append("\",");
        sb.append("\"position\":");
        appendPosition(sb, entity.getX(), entity.getY(), entity.getZ());
        sb.append(",\"block\":");
        appendBlock(sb, world, blockPos);
        sb.append('}');
    }

    private static void appendPosition(StringBuilder sb, double x, double y, double z) {
        sb.append("{\"x\":").append(formatCoordinate(x))
                .append(",\"y\":").append(formatCoordinate(y))
                .append(",\"z\":").append(formatCoordinate(z))
                .append('}');
    }

    private static void appendBlock(StringBuilder sb, ServerWorld world, BlockPos pos) {
        sb.append("{\"id\":\"").append(jsonEscape(getBlockId(world, pos))).append("\",");
        sb.append("\"position\":");
        sb.append("{\"x\":").append(pos.getX())
                .append(",\"y\":").append(pos.getY())
                .append(",\"z\":").append(pos.getZ())
                .append("}}");
    }

    private static String getBlockId(ServerWorld world, BlockPos pos) {
        return Registries.BLOCK.getId(world.getBlockState(pos).getBlock()).toString();
    }

    private static String formatCoordinate(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String jsonEscape(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
