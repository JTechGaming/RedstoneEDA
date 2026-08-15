package com.cybrisoft.redstoneeda.io;

import com.cybrisoft.redstoneeda.debugging.ServerEntityTracker;
import com.cybrisoft.redstoneeda.debugging.SessionTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.ArrayUtils;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SessionLogger {
    /**
     *
     * File format
     *
     * jump table
     * #
     * int16 version
     * int32 ticks
     * #
     *
     * following is repeated for tick amount
     * #
     * int32 tickFirstByte
     * int32 size
     * int16 entries
     * #
     * Size simply stores the full byte length of all entries in this tick combined
     * Entries stores how many changes are stored in that tick
     *
     * The first byte is counted from the first byte after the jump table
     * so first to go to the first byte of a given tick would be:
     *
     * A tick is simply stored as a consecutive stream of tick entries, so the first byte of a tick
     * is the first byte of the first tick entry.
     * So to read a given tick, you would start at byte:
     * 5 + ticks * 10 + tickFirstByte
     * And should hopefully end at:
     * 5 + ticks * 10 + tickFirstByte + size
     * So if there is only one tick, then it would be:
     * 5 + 10 + 0 = 15
     * Because the first tick is always the byte after the end of the jump table, and byte indexing starts at 0
     * so the version number is stored at byte 0
     *
     * Then each tick entry looks like this:
     * #
     * int16 type
     * int16 size
     * int32 data
     *
     * (for entity, there is a UUID (string))
     * (for block and block entity types, there is a BlockPos (long))
     *
     * int32 properties
     *
     * #
     * The type is split up into two 8-bit numbers. The first part can be 0: Entity, 1: block, or 2: block entity
     * The second part is to store what this entry represents. 0: create, 1: modify, 2: destroy
     * Size simply stores the full byte length of this one entry, including the type, size, data and properties values
     * Properties is essentially a list of flags using binary or. Each property has a flag
     * Then how each property stores data can differ. For instance, a blockpos might just be a single long,
     * whereas a Vec3d might be 3 consecutive floats. The reader parses the property flag list, and just reads
     * the expected bytes in order. For example, say you have a blockstate and then a Vec3d of three consecutive floats.
     * The reader now knows to read a long, float, float, float, and put them in datatypes BlockPos and Vec3d.
     *
     * Different from properties, data can store a regular 32-bit number to be used in parsing. For the creation of an entity
     * for example, the parser would have to know what entity type it is. This would be stored in data. The same thing goes for the
     * block type (block id).
     *
     * The flag format depends on the type.
     * The entity type might contain flags like x pos, y pos, z pos, blockpos, yaw, pitch, health etc.
     * The block type will have flags depending on its blockstate.
     *
     * Entity format:
     * (0)  1:          health (float)
     * (1)  2:          x pos (float)
     * (2)  4:          y pos (float)
     * (3)  8:          z pos (float)
     * (4)  16:         yaw
     * (5)  32:         pitch
     * (6)  64:         reserved
     * (7)  128:        reserved
     * (8)  256:        reserved
     * (9)  512:        reserved
     * (10) 1024:       reserved
     * (11) 2048:       reserved
     * (12) 4096:       reserved
     * (13) 8192:       reserved
     * (14) 16384:      reserved
     * (15) 32768:      reserved
     * (16) 65536:      reserved
     * (17) 131072:     reserved
     * (18) 262144:     reserved
     * (19) 524288:     reserved
     * (20) 1048576:    reserved
     * (21) 2097152:    reserved
     * (22) 4194304:    reserved
     * (23) 8388608:    reserved
     * (24) 16777216:   reserved
     * (25) 33554432:   reserved
     * (26) 67108864:   reserved
     * (27) 134217728:  reserved
     * (28) 268435456:  reserved
     * (29) 536870912:  reserved
     * (30) 1073741824: reserved
     * (31) 2147483648: reserved
     *
     * Block format:
     * (0)  1:          health (int)
     * (1)  2:          x pos (float)
     * (2)  4:          y pos (float)
     * (3)  8:          z pos (float)
     * (4)  16:         blockpos (long)
     * (5)  32:         reserved
     * (6)  64:         reserved
     * (7)  128:        reserved
     * (8)  256:        reserved
     * (9)  512:        reserved
     * (10) 1024:       reserved
     * (11) 2048:       reserved
     * (12) 4096:       reserved
     * (13) 8192:       reserved
     * (14) 16384:      reserved
     * (15) 32768:      reserved
     * (16) 65536:      reserved
     * (17) 131072:     reserved
     * (18) 262144:     reserved
     * (19) 524288:     reserved
     * (20) 1048576:    reserved
     * (21) 2097152:    reserved
     * (22) 4194304:    reserved
     * (23) 8388608:    reserved
     * (24) 16777216:   reserved
     * (25) 33554432:   reserved
     * (26) 67108864:   reserved
     * (27) 134217728:  reserved
     * (28) 268435456:  reserved
     * (29) 536870912:  reserved
     * (30) 1073741824: reserved
     * (31) 2147483648: reserved
     *
     * Block Entity format:
     * (0)  1:          health (int)
     * (1)  2:          x pos (float)
     * (2)  4:          y pos (float)
     * (3)  8:          z pos (float)
     * (4)  16:         blockpos (long)
     * (5)  32:         reserved
     * (6)  64:         reserved
     * (7)  128:        reserved
     * (8)  256:        reserved
     * (9)  512:        reserved
     * (10) 1024:       reserved
     * (11) 2048:       reserved
     * (12) 4096:       reserved
     * (13) 8192:       reserved
     * (14) 16384:      reserved
     * (15) 32768:      reserved
     * (16) 65536:      reserved
     * (17) 131072:     reserved
     * (18) 262144:     reserved
     * (19) 524288:     reserved
     * (20) 1048576:    reserved
     * (21) 2097152:    reserved
     * (22) 4194304:    reserved
     * (23) 8388608:    reserved
     * (24) 16777216:   reserved
     * (25) 33554432:   reserved
     * (26) 67108864:   reserved
     * (27) 134217728:  reserved
     * (28) 268435456:  reserved
     * (29) 536870912:  reserved
     * (30) 1073741824: reserved
     * (31) 2147483648: reserved
     */

    public enum Type {
        ENTITY(1),
        BLOCK(2),
        BLOCK_ENTITY(4)
        ;

        private final int bitIndex;

        Type(int bitIndex) {
            this.bitIndex = bitIndex;
        }

        public int id() {
            return bitIndex;
        }
    }

    public enum EntryType {
        CREATE(16),
        MODIFY(32),
        DESTROY(64)
        ;

        private final int bitIndex;

        EntryType(int bitIndex) {
            this.bitIndex = bitIndex;
        }

        public int id() {
            return bitIndex;
        }
    }

    public enum EntityFlags {
        HEALTH(0),
        X(1),
        Y(2),
        Z(3),
        YAW(4),
        PITCH(5)
        ;

        private final int bitIndex;

        EntityFlags(int bitIndex) {
            this.bitIndex = bitIndex;
        }

        public int id() {
            return bitIndex;
        }
    }

    static List<Byte> header;
    static List<Byte> data;

    public static void write(UUID project, SessionTracker.TickReport report) throws IOException {
        short entries = (short) (report.getRecordedBlockEntities().size() + report.getRecordedBlocks().size() + report.getRecordedEntities().size());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(baos)) {

            for (ServerEntityTracker.Entry entity : report.getRecordedEntities()) {
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     DataOutputStream dos = new DataOutputStream(bos)) {

                    dos.writeInt(getEntityId(entity.getEntityType()));

                    String uuid = entity.getUuid().toString();
                    dos.writeChars(uuid);

                    BitSet f = entity.flags;
                    if (hasEntityFlag(f, EntityFlags.HEALTH)) {
                        dos.writeFloat(entity.getHealth());
                    }
                    if (hasEntityFlag(f, EntityFlags.X)) {
                        dos.writeDouble(entity.getX());
                    }
                    if (hasEntityFlag(f, EntityFlags.Y)) {
                        dos.writeDouble(entity.getY());
                    }
                    if (hasEntityFlag(f, EntityFlags.Z)) {
                        dos.writeDouble(entity.getZ());
                    }
                    if (hasEntityFlag(f, EntityFlags.YAW)) {
                        dos.writeFloat(entity.getYaw());
                    }
                    if (hasEntityFlag(f, EntityFlags.PITCH)) {
                        dos.writeFloat(entity.getPitch());
                    }

                    out.writeShort(
                            Type.ENTITY.id() | entity.getType().id()
                    );

                    byte[] bytes = bos.toByteArray();
                    out.writeShort(4 + bytes.length); // length of type short, size short and bos length

                    out.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            for (SessionTracker.BlockReport block : report.getRecordedBlocks()) {

            }
            for (SessionTracker.BlockEntityReport blockEntity : report.getRecordedBlockEntities()) {

            }

            byte[] bytes = baos.toByteArray();

            updateheader(bytes.length, entries);

            for (byte b : bytes) {
                data.add(b);
            }
        }
    }

    private static void updateheader(int requiredSize, short entries) {
        int size = header.size() + 12;
        ByteBuffer headerBuf = ByteBuffer.allocate(size);

        headerBuf.put(ArrayUtils.toPrimitive(header.toArray(new Byte[0])));

        short version = 1;
        int ticks = 0;

        if (header.size() >= 6) { // 6 bytes = short (2) + int (4)
            version = headerBuf.getShort(0);
            ticks = headerBuf.getInt(2);

            headerBuf.putInt(2, ticks);

            headerBuf.position(header.size());
        } else {
            headerBuf.position(0);
            headerBuf.putShort(version);
            headerBuf.putInt(ticks);
        }

        headerBuf.putInt(data.size());
        headerBuf.putInt(requiredSize);
        headerBuf.putShort(entries);

        header.clear();
        headerBuf.flip();

        while (headerBuf.hasRemaining()) {
            header.add(headerBuf.get());
        }
    }

    public static void parseTick(UUID project, ServerWorld world, int tick) {
        ByteBuffer headerBuf = ByteBuffer.wrap(ArrayUtils.toPrimitive(header.toArray(new Byte[0])));
        ByteBuffer buf = ByteBuffer.wrap(ArrayUtils.toPrimitive(data.toArray(new Byte[0])));

        // read the header
        if (headerBuf.hasRemaining()) {
            short version = headerBuf.getShort();
            int ticks = headerBuf.getInt();

            for (int i=0; i<ticks; i++) {
                int tickFirstByte = headerBuf.getInt();
                int size = headerBuf.getInt();
                int entries = headerBuf.getShort();

                if (i != tick) continue;

                parseTickEntries(world, buf, tickFirstByte, size, entries);

                return;
            }
        }
    }

    private static boolean hasEntityFlag(BitSet flags, EntityFlags flag) {
        return flags.get(flag.id());
    }

    private static void parseTickEntries(ServerWorld world, ByteBuffer buf, int tickFirstByte, int tickSize, int entries) {
        // buf starts at 0 again and is tickSize long
        for (int i=0; i<entries; i++) {
            short type = buf.getShort();
            short size = buf.getShort();
            int data = buf.getInt();

            BitSet typeData = BitSet.valueOf(new long[] {type});

            Entity entity = null;
            BlockPos blockPos = null;
            if (typeData.get(0)) {
                String uuid = readString(buf);
                entity = world.getEntity(UUID.fromString(uuid));
            } else if (typeData.get(1) || typeData.get(2)) {
                blockPos = readPos(buf);
            }

            boolean create  = typeData.get(8);
            boolean modify  = typeData.get(9);
            boolean destroy = typeData.get(10);

            if (create) {
                if (typeData.get(0)) {
                    entity = instantiateEntity(data, world); // in the entity case the data segment contains the entity ID
                }
            }

            if (destroy) {
                if (typeData.get(0) && entity != null) {
                    entity.remove(Entity.RemovalReason.DISCARDED);
                    continue;
                }
            }

            int properties = buf.getInt();

            BitSet flags = BitSet.valueOf(new long[] {properties});

            if (!(modify || create)) continue;

            if (typeData.get(0)) { // Entity
                if (entity == null) throw new IllegalStateException();

                double x = entity.getX();
                double y = entity.getY();
                double z = entity.getZ();
                boolean posChanged = false;

                if (hasEntityFlag(flags, EntityFlags.HEALTH)) { // health
                    float health = buf.getFloat();
                    if (entity instanceof LivingEntity e) {
                        e.setHealth(health);
                    }
                }
                if (hasEntityFlag(flags, EntityFlags.X)) { // x
                    x = buf.getDouble();
                    posChanged = true;
                }
                if (hasEntityFlag(flags, EntityFlags.Y)) { // y
                    y = buf.getDouble();
                    posChanged = true;
                }
                if (hasEntityFlag(flags, EntityFlags.Z)) { // z
                    z = buf.getDouble();
                    posChanged = true;
                }
                if (hasEntityFlag(flags, EntityFlags.YAW)) { // yaw
                    float yaw = buf.getFloat();
                    entity.setYaw(yaw);
                }
                if (hasEntityFlag(flags, EntityFlags.PITCH)) { // pitch
                    float pitch = buf.getFloat();
                    entity.setPitch(pitch);
                }

                if (posChanged) {
                    entity.setPos(x, y, z);
                }
            } else if (typeData.get(1)) { // Block

            } else if (typeData.get(2)) { // Block Entity

            }
        }
    }

    private static int getEntityId(EntityType<?> type) {
        return Registries.ENTITY_TYPE.getRawId(type);
    }

    private static @Nullable Entity instantiateEntity(int entityId, ServerWorld world) {
        if (entityId >= Registries.ENTITY_TYPE.size()) return null;
        EntityType<?> type = Registries.ENTITY_TYPE.get(entityId);
        return type.create(world, SpawnReason.COMMAND);
    }

    private static void writeString(ByteBuffer buf, String string) {
        buf.putInt(string.length());
        buf.put(string.getBytes(StandardCharsets.UTF_8));
    }

    private static String readString(ByteBuffer buf) {
        int length = buf.getInt();
        byte[] stringBytes = new byte[length];
        buf.get(stringBytes);

        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    private static void writePos(ByteBuffer buf, BlockPos pos) {
        buf.putLong(pos.asLong());
    }

    private static BlockPos readPos(ByteBuffer buf) {
        return BlockPos.fromLong(buf.getLong());
    }
}
