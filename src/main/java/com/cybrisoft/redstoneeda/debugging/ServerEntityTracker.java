package com.cybrisoft.redstoneeda.debugging;

import com.cybrisoft.redstoneeda.io.SessionLogger;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.BlockPos;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

    public static Set<Entry> getTrackingEntities() {
        return trackingEntities;
    }

    public static void updatePos(int id, double x, double y, double z) {
        Entry entry = find(id);
        if (entry == null) return;
        entry.setPos(x, y, z);
    }

    public static void updateHealth(int id, float health) {
        Entry entry = find(id);
        if (entry == null) return;
        entry.setHealth(health);
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
        private float health;
        private double x;
        private double y;
        private double z;
        private BlockPos blockPos;

        private UUID uuid;
        private EntityType<?> entityType;

        private float yaw;
        private float pitch;

        private SessionLogger.EntryType type;

        public BitSet flags = new BitSet(8);

        private boolean changed = false;

        public Entry(int id, EntityType<?> entityType, UUID uuid) {
            this.id = id;
            this.type = SessionLogger.EntryType.CREATE;
            this.entityType = entityType;
            this.uuid = uuid;
        }

        public int getId() {
            return id;
        }

        public float getHealth() {
            return health;
        }

        public UUID getUuid() {
            return uuid;
        }

        public void setHealth(float health) {
            this.health = health;
            flags.set(SessionLogger.EntityFlags.HEALTH.id());
        }

        public void setPos(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;

            blockPos = BlockPos.ofFloored(x, y, z);
            flags.set(SessionLogger.EntityFlags.X.id());
            flags.set(SessionLogger.EntityFlags.Y.id());
            flags.set(SessionLogger.EntityFlags.Z.id());
            markChanged(); // root class already checks if changed
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

        public BlockPos getBlockPos() {
            return blockPos;
        }

        public float getYaw() {
            return yaw;
        }

        public void setYaw(float yaw) {
            if (this.yaw != yaw) {
                markChanged();
                flags.set(SessionLogger.EntityFlags.YAW.id());
                this.yaw = yaw;
            }
        }

        public float getPitch() {
            return pitch;
        }

        public SessionLogger.EntryType getType() {
            return type;
        }

        public void setType(SessionLogger.EntryType type) {
            this.type = type;
        }

        public void setPitch(float pitch) {
            if (this.pitch != pitch) {
                markChanged();
                flags.set(SessionLogger.EntityFlags.PITCH.id());
                this.pitch = pitch;
            }
        }

        public boolean hasChanged() {
            return changed;
        }

        public void markChanged() {
            this.changed = true;
        }

        public void close() {
            this.changed = false;
        }

        public EntityType<?> getEntityType() {
            return entityType;
        }
    }
}
