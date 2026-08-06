package com.cybrisoft.redstoneeda.debugging;

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

    public static void tickSession(UUID project) {
        if (server == null) return;

        Session session = sessions.get(project);
        if (session == null) return;
        if (!session.isRunning) return;

        session.setLastTick(server.getTicks());
    }

    public static void pauseSession(UUID project) {
        if (server == null) return;

        Session session = sessions.get(project);
        if (session == null) return;

        session.setRunning(true);
        session.setLastTick(server.getTicks());
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
}
