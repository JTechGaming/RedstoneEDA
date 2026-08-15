package com.cybrisoft.redstoneeda;

import com.cybrisoft.redstoneeda.breakpoints.Breakpoint;
import com.cybrisoft.redstoneeda.debugging.SessionTracker;
import com.cybrisoft.redstoneeda.io.ServerProjectStorage;
import com.cybrisoft.redstoneeda.managers.ServerDebugManager;
import com.cybrisoft.redstoneeda.networking.C2S.C2SBreakpointPacket;
import com.cybrisoft.redstoneeda.networking.C2S.C2SEditProjectPacket;
import com.cybrisoft.redstoneeda.networking.C2S.C2SInfoPacket;
import com.cybrisoft.redstoneeda.networking.C2S.C2SOpenProjectPacket;
import com.cybrisoft.redstoneeda.networking.S2C.S2CQueryProjectsPacket;
import com.cybrisoft.redstoneeda.networking.S2C.S2CSyncProjectPacket;
import com.cybrisoft.redstoneeda.networking.S2C.S2CTriggeredBreakpointPacket;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Redstoneeda implements ModInitializer {
    public static final String MOD_ID = "redstoneeda";
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final String version = "1.0.0-1.21.11+d356";

    public static boolean debugMode = false;

    public static Identifier identifier(String name) {
        return Identifier.of(MOD_ID, name);
    }

    @Override
    public void onInitialize() {
        // C2S
        PayloadTypeRegistry.playC2S().register(C2SBreakpointPacket.ID, C2SBreakpointPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SEditProjectPacket.ID, C2SEditProjectPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SOpenProjectPacket.ID, C2SOpenProjectPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SInfoPacket.ID, C2SInfoPacket.CODEC);

        // S2C
        PayloadTypeRegistry.playS2C().register(S2CSyncProjectPacket.ID, S2CSyncProjectPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(S2CQueryProjectsPacket.ID, S2CQueryProjectsPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(S2CTriggeredBreakpointPacket.ID, S2CTriggeredBreakpointPacket.CODEC);

        // Server Receivers
        ServerPlayNetworking.registerGlobalReceiver(C2SBreakpointPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                Breakpoint bp = payload.bp();
                UUID uuid = bp.getUuid();

                UUID projectUUID = ServerDebugManager.getPlayerSession(context.player().getUuid());
                Project project = ServerDebugManager.getSession(projectUUID);

                if (project == null) return;

                boolean exists = false;

                for (int i = 0; i< project.getBreakpoints().size(); i++) {
                    Breakpoint breakpoint = project.getBreakpoints().get(i);
                    if (breakpoint.getUuid().equals(uuid)) {
                        project.getBreakpoints().set(i, bp);
                        exists = true;
                    }
                }

                if (!exists) {
                    project.getBreakpoints().add(bp);
                }

                ServerProjectStorage.saveProject(project);

                for (UUID player : ServerDebugManager.getPlayerSessions().keySet()) {
                    if (player.equals(context.player().getUuid())) continue;
                    if (ServerDebugManager.getPlayerSessions().get(player).equals(projectUUID)) {
                        ServerPlayerEntity playerEntity = context.server().getPlayerManager().getPlayer(context.player().getUuid());
                        if (playerEntity == null) continue;
                        ServerPlayNetworking.send(playerEntity, new S2CSyncProjectPacket(project));
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2SEditProjectPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                Project project = payload.project();
                UUID playerUUID = context.player().getUuid();

                UUID projectUUID = ServerDebugManager.createProject(project);

                ServerDebugManager.setPlayerSession(playerUUID, projectUUID);

                ServerProjectStorage.saveProject(project);

                for (UUID player : ServerDebugManager.getPlayerSessions().keySet()) {
                    if (player.equals(playerUUID)) continue;
                    if (ServerDebugManager.getPlayerSessions().get(player).equals(projectUUID)) {
                        ServerPlayerEntity playerEntity = context.server().getPlayerManager().getPlayer(player);
                        if (playerEntity == null) continue;
                        ServerPlayNetworking.send(playerEntity, new S2CSyncProjectPacket(project));
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2SOpenProjectPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                UUID playerUUID = context.player().getUuid();
                UUID projectUUID = payload.project();

                ServerDebugManager.setPlayerSession(playerUUID, projectUUID);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2SInfoPacket.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (payload.op().equals(C2SInfoPacket.Ops.QUERY.id())) {
                    Map<UUID, String> projects = new HashMap<>();
                    for (Project project : ServerDebugManager.getCurrentSessions().values()) {
                        projects.put(project.getUuid(), project.getName());
                    }
                    ServerPlayNetworking.send(context.player() , new S2CQueryProjectsPacket(projects));
                } else if (payload.op().equals(C2SInfoPacket.Ops.REQUEST.id())) {
                    if (!payload.data().isBlank()) {
                        UUID uuid = UUID.fromString(payload.data());

                        Project project = ServerDebugManager.getSession(uuid);
                        if (project == null) return;

                        ServerDebugManager.setPlayerSession(context.player().getUuid(), uuid);
                        ServerPlayNetworking.send(context.player() , new S2CSyncProjectPacket(project));
                    }
                } else if (payload.op().equals(C2SInfoPacket.Ops.CLEAR.id())) {
                    ServerDebugManager.clearPlayerSession(context.player().getUuid());
                } else if (payload.op().equals(C2SInfoPacket.Ops.TOGGLE_FREEZE.id())) {
                    UUID uuid = UUID.fromString(payload.data());

                    Project project = ServerDebugManager.getSession(uuid);
                    if (project == null) return;

                    project.setFrozen(!project.isFrozen()); // todo figure out if i want to sync this to client
                } else if (payload.op().equals(C2SInfoPacket.Ops.TOGGLE_DEBUG.id())) {
                    if (!payload.data().isBlank()) {
                        UUID uuid = UUID.fromString(payload.data());
                        if (!SessionTracker.getSessions().contains(uuid)) return;
                        if (SessionTracker.sessionActive(uuid)) {
                            SessionTracker.pauseSession(uuid);
                        } else {
                            SessionTracker.startSession(uuid);
                        }
                    }
                }
            });
        });

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            List<Project> projectList = ServerProjectStorage.queryProjects();
            for (Project project : projectList) {
                ServerDebugManager.createProject(project);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register((server) -> {
            ServerDebugManager.tick(server);

            SessionTracker.tickSessions();
        });
    }
}
