package com.cybrisoft.redstoneeda.networking.S2C;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.breakpoints.Breakpoint;
import com.cybrisoft.redstoneeda.networking.NetworkingConstants;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

import java.util.*;

public record S2CQueryProjectsPacket(Map<UUID, String> projects) implements CustomPayload {
    public static final Id<S2CQueryProjectsPacket> ID = new Id<>(NetworkingConstants.S2C_QUERY_PROJECTS);

    public static final PacketCodec<PacketByteBuf, Map<UUID, String>> PACKET_CODEC = new PacketCodec<PacketByteBuf, Map<UUID, String>>() {
        public Map<UUID, String> decode(PacketByteBuf buf) {
            int size = buf.readInt();
            Map<UUID, String> result = new HashMap<>();

            for (int i=0; i<size; i++) {
                UUID uuid = buf.readUuid();
                String name = buf.readString();
                result.put(uuid, name);
            }

            return result;
        }

        public void encode(PacketByteBuf buf, Map<UUID, String> projects) {
            buf.writeInt(projects.size());
            for (UUID uuid : projects.keySet()) {
                String name = projects.get(uuid);
                buf.writeUuid(uuid);
                buf.writeString(name);
            }
        }
    };

    public static final PacketCodec<RegistryByteBuf, S2CQueryProjectsPacket> CODEC = PacketCodec.tuple(
            PACKET_CODEC, S2CQueryProjectsPacket::projects,
            S2CQueryProjectsPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
