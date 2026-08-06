package com.cybrisoft.redstoneeda.networking.C2S;

import com.cybrisoft.redstoneeda.networking.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.UUID;

public record C2SOpenProjectPacket(UUID project) implements CustomPayload {
    public static final Id<C2SOpenProjectPacket> ID = new Id<>(NetworkingConstants.C2S_OPEN_PROJECT);
    public static final PacketCodec<RegistryByteBuf, C2SOpenProjectPacket> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, C2SOpenProjectPacket::project,
            C2SOpenProjectPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
