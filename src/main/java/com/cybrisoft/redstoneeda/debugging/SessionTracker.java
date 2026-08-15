package com.cybrisoft.redstoneeda.debugging;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.io.SessionLogger;
import com.cybrisoft.redstoneeda.managers.ServerDebugManager;
import net.minecraft.server.MinecraftServer;

import java.util.*;

public class SessionTracker {
    private static final Map<UUID, Session> sessions = new HashMap<>();

    private static MinecraftServer server = null;

    public static void init(MinecraftServer server) {
        SessionTracker.server = server;
    }

    public static void setupSession(UUID project, Session session) {
        sessions.put(project, session);
    }

    public static void startSession(UUID project) {
        if (server == null) return;

        Session session = sessions.get(project);
        if (session == null) return;

        session.setStartTick(server.getTicks());

        session.setRunning(true);
    }

    private static void tickSession(UUID project) {
        if (server == null) return;

        Session session = sessions.get(project);
        if (session == null) return;
        if (!session.isRunning) return;

        session.setLastTick(server.getTicks());
    }

    public static void tickSessions() {
        if (server == null) return;

        for (UUID session : sessions.keySet()) {
            // tick session
            SessionTracker.tickSession(session);
        }

        TickReport report = new TickReport();

        int tick = server.getTicks();

        // tick entity trackers
        for (ServerEntityTracker.Entry trackedEntity : ServerEntityTracker.getTrackingEntities()) {
            if (!trackedEntity.hasChanged()) continue;

            for (UUID session : sessions.keySet()) {
                Project project = ServerDebugManager.getSession(session);

                if (project.isIn(trackedEntity.getBlockPos())) {
                    report.recordEntity(trackedEntity);
                }
            }

            trackedEntity.close();
        }

        // tick block trackers

        // tick block entity trackers

        // submit tickReport
        // SessionLogger.write(report);
    }

    public static void pauseSession(UUID project) {
        if (server == null) return;

        Session session = sessions.get(project);
        if (session == null) return;

        session.setRunning(true);
        session.setLastTick(server.getTicks());
    }

    public static boolean sessionActive(UUID project) {
        Session session = sessions.get(project);
        if (session == null) return false;

        return session.isRunning;
    }

    public static Set<UUID> getSessions() {
        return sessions.keySet();
    }

    public static void destroySession(UUID project) {
        sessions.remove(project);
    }

    public static class Session {
        long startTick = -1;
        long lastTick = -1;
        boolean isRunning = false;

        public long getStartTick() {
            return startTick;
        }

        public void setStartTick(long startTick) {
            this.startTick = startTick;
        }

        public long getLastTick() {
            return lastTick;
        }

        public void setLastTick(long lastTick) {
            this.lastTick = lastTick;
        }

        public boolean isRunning() {
            return isRunning;
        }

        public void setRunning(boolean running) {
            isRunning = running;
        }
    }

    public static class BlockReport {

    }

    public static class BlockEntityReport {

    }

    public static class TickReport {
        private final Set<ServerEntityTracker.Entry> recordedEntities = new HashSet<>();
        private final Set<BlockReport> recordedBlocks = new HashSet<>();
        private final Set<BlockEntityReport> recordedBlockEntities = new HashSet<>();

        public void recordEntity(ServerEntityTracker.Entry entityReport) {
            recordedEntities.add(entityReport);
        }

        public void recordBlock(BlockReport blockReport) {
            recordedBlocks.add(blockReport);
        }

        public void recordBlockEntity(BlockEntityReport blockEntityReport) {
            recordedBlockEntities.add(blockEntityReport);
        }

        public Set<ServerEntityTracker.Entry> getRecordedEntities() {
            return recordedEntities;
        }

        public Set<BlockReport> getRecordedBlocks() {
            return recordedBlocks;
        }

        public Set<BlockEntityReport> getRecordedBlockEntities() {
            return recordedBlockEntities;
        }
    }
}
