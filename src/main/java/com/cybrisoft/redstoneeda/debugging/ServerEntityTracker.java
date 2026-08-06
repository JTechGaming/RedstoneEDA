package com.cybrisoft.redstoneeda.debugging;

import java.util.HashSet;
import java.util.Set;

public class ServerEntityTracker {
    private static final Set<Entry> trackingEntities = new HashSet<>();

    public static void register(Entry entity) {
        trackingEntities.add(entity);
    }

    public static void remove(int id) {
        for (Entry entry : trackingEntities) {
            if (entry.id == id) {
                trackingEntities.remove(entry);
                return;
            }
        }
    }

    public static Entry find(int id) {
        for (Entry entry : trackingEntities) {
            if (entry.id == id) {
                return entry;
            }
        }
        return null;
    }

    public static void updatePos(int id, double x, double y, double z) {
        Entry entry = find(id);
        if (entry == null) return;
        entry.setPos(x, y, z);
    }

    public static void updateYaw(int id, float yaw) {
        Entry entry = find(id);
        if (entry == null) return;
        entry.setYaw(yaw);
    }

    public static void updatePitch(int id, float pitch) {
        Entry entry = find(id);
        if (entry == null) return;
        entry.setPitch(pitch);
    }

    public static class Entry {
        private final int id;
        private double x;
        private double y;
        private double z;

        private float yaw;
        private float pitch;

        public Entry(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getZ() {
            return z;
        }

        public float getYaw() {
            return yaw;
        }

        public void setYaw(float yaw) {
            this.yaw = yaw;
        }

        public float getPitch() {
            return pitch;
        }

        public void setPitch(float pitch) {
            this.pitch = pitch;
        }
    }
}
