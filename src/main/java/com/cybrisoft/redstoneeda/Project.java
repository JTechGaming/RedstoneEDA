package com.cybrisoft.redstoneeda;

import com.cybrisoft.redstoneeda.breakpoints.Breakpoint;
import com.cybrisoft.redstoneeda.io.ServerProjectStorage;
import com.cybrisoft.redstoneeda.managers.ServerDebugManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Project {
    private UUID uuid;
    private String name;
    private RegistryKey<World> world;
    private List<Breakpoint> serverBreakpoints;
    private BlockPos min;
    private BlockPos max;
    private boolean isFrozen = false;

    public Project(UUID uuid, String name, RegistryKey<World> world, List<Breakpoint> serverBreakpoints) {
        this.uuid = uuid;
        this.name = name;
        this.world = world;
        this.serverBreakpoints = serverBreakpoints;
    }

    public Project(UUID uuid, String name, RegistryKey<World> world, List<Breakpoint> serverBreakpoints, BlockPos min, BlockPos max) {
        this.uuid = uuid;
        this.name = name;
        this.world = world;
        this.serverBreakpoints = serverBreakpoints;
        this.min = min;
        this.max = max;
    }

    public Project(UUID uuid, String name, RegistryKey<World> world, List<Breakpoint> serverBreakpoints, BlockPos min, BlockPos max, boolean isFrozen) {
        this.uuid = uuid;
        this.name = name;
        this.world = world;
        this.serverBreakpoints = serverBreakpoints;
        this.min = min;
        this.max = max;
        this.isFrozen = false; // todo test if this fixes things
    }

    public UUID getUuid() {
        return uuid;
    }

    public RegistryKey<World> getWorld() {
        return world;
    }

    public List<Breakpoint> getBreakpoints() {
        return serverBreakpoints;
    }

    public void addBreakpoint(Breakpoint bp) {
        serverBreakpoints.add(bp);
    }

    public void removeBreakpoint(Breakpoint bp) {
        serverBreakpoints.remove(bp);
    }

    public boolean containsBreakpoint(UUID uuid) {
        for (Breakpoint bp : serverBreakpoints) {
            if (bp.getUuid().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BlockPos getMin() {
        return min;
    }

    public BlockPos getMax() {
        return max;
    }

    public void setMin(BlockPos min) {
        this.min = min;
    }

    public void setMax(BlockPos max) {
        this.max = max;
    }

    public boolean isFrozen() {
        return isFrozen;
    }

    public void setFrozen(boolean frozen) {
        isFrozen = frozen;

        if (!isFrozen) {
            ServerDebugManager.unfreezeProject(this);
        }

        ServerProjectStorage.saveProject(this);
    }

    public boolean isIn(BlockPos pos) {
        return  pos.getX() >= Math.min(min.getX(), max.getX()) &&
                pos.getX() <= Math.max(min.getX(), max.getX()) &&
                pos.getY() >= Math.min(min.getY(), max.getY()) &&
                pos.getY() <= Math.max(min.getY(), max.getY()) &&
                pos.getZ() >= Math.min(min.getZ(), max.getZ()) &&
                pos.getZ() <= Math.max(min.getZ(), max.getZ());
    }

    public static final PacketCodec<PacketByteBuf, Project> PACKET_CODEC = new PacketCodec<PacketByteBuf, Project>() {
        public Project decode(PacketByteBuf buf) {
            UUID uuid = buf.readUuid();
            String name = buf.readString();
            RegistryKey<World> worldRegistryKey = buf.readRegistryKey(RegistryKeys.WORLD);
            List<Breakpoint> breakpoints = new ArrayList<>();
            int breakpointCount = buf.readInt();
            for (int i=0; i<breakpointCount; i++) {
                breakpoints.add(Breakpoint.PACKET_CODEC.decode(buf));
            }
            if (buf.isReadable()) {
                return new Project(uuid, name, worldRegistryKey, breakpoints, buf.readBlockPos(), buf.readBlockPos());
            }
            return new Project(uuid, name, worldRegistryKey, breakpoints);
        }

        public void encode(PacketByteBuf buf, Project project) {
            buf.writeUuid(project.getUuid());
            buf.writeString(project.getName());
            buf.writeRegistryKey(project.getWorld());
            buf.writeInt(project.getBreakpoints().size());
            for (Breakpoint bp : project.getBreakpoints()) {
                Breakpoint.PACKET_CODEC.encode(buf, bp);
            }
            if (project.getMin() != null) {
                buf.writeBlockPos(project.getMin());
                buf.writeBlockPos(project.getMax());
            }
        }
    };
}
