package com.cybrisoft.redstoneeda.managers;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.breakpoints.Breakpoint;
import com.cybrisoft.redstoneeda.breakpoints.BreakpointResult;
import com.cybrisoft.redstoneeda.mixin.ServerWorldMixin;
import com.cybrisoft.redstoneeda.networking.S2C.S2CSyncProjectPacket;
import com.cybrisoft.redstoneeda.networking.S2C.S2CTriggeredBreakpointPacket;
import com.cybrisoft.redstoneeda.util.FrozenNeighborUpdater;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Not marked EnvType.SERVER because of common-side mixins,
 * but its methods are only called from the server so it can at any
 * point be assumed it is server only.
 */
public class ServerDebugManager {
    private static Map<UUID, Project> currentSessions = new HashMap<>();

    private static Map<UUID, UUID> playerSessionTracker = new HashMap<>();

    public static void setPlayerSession(UUID player, UUID session) {
        playerSessionTracker.put(player, session);
    }

    public static UUID getPlayerSession(UUID player) {
        return playerSessionTracker.get(player);
    }

    public static void clearPlayerSession(UUID player) {
        playerSessionTracker.remove(player);
    }

    public static Map<UUID, UUID> getPlayerSessions() {
        return playerSessionTracker;
    }

    public static UUID createProject(Project session) {
        currentSessions.put(session.getUuid(), session);
        return session.getUuid();
    }

    public static void deleteProject(UUID uuid) {
        currentSessions.remove(uuid);
    }

    public static Project getSession(UUID uuid) {
        return currentSessions.get(uuid);
    }

    public static Map<UUID, Project> getCurrentSessions() {
        return currentSessions;
    }

    private static MinecraftServer server = null;

    public static void tick(MinecraftServer server) {
        ServerDebugManager.server = server;
        if (currentSessions.isEmpty()) return;

        server.execute(() -> {
            for (Project session : currentSessions.values()) {
                for (Breakpoint bp : session.getBreakpoints()) {
                    if (!bp.isActive()) continue;

                    if (server.getWorld(session.getWorld()) == null) continue;

                    BreakpointResult breakpointResult = bp.getCondition().evaluate(server.getWorld(session.getWorld()));

                    if (breakpointResult == null) continue;

                    if (bp.isShouldPauseGame()) {
                        session.setFrozen(!session.isFrozen()); // todo figure out if i want to sync this to client

                        for (UUID player : ServerDebugManager.getPlayerSessions().keySet()) {
                            if (ServerDebugManager.getPlayerSessions().get(player).equals(session.getUuid())) {
                                ServerPlayerEntity playerEntity = server.getPlayerManager().getPlayer(player);
                                if (playerEntity == null) continue;
                                ServerPlayNetworking.send(playerEntity, new S2CTriggeredBreakpointPacket(breakpointResult));
                            }
                        }
                    }
                    if (bp.isDisableOnTrigger()) {
                        bp.setActive(false);
                    }

                    break;
                }
            }
        });
    }

    public static void unfreezeProject(Project project) {
        if (server == null) return;

        ServerWorld world = server.getWorld(project.getWorld());

        if (world == null) return;

        FrozenNeighborUpdater updater = (FrozenNeighborUpdater) world.neighborUpdater;
        updater.redstoneeda$unfreeze();
    }
}
