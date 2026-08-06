package com.cybrisoft.redstoneeda.networking.S2C;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.networking.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record S2CSyncProjectPacket(Project project) implements CustomPayload {
    public static final Id<S2CSyncProjectPacket> ID = new Id<>(NetworkingConstants.S2C_SYNC_PROJECT);
    public static final PacketCodec<RegistryByteBuf, S2CSyncProjectPacket> CODEC = PacketCodec.tuple(
            Project.PACKET_CODEC, S2CSyncProjectPacket::project,
            S2CSyncProjectPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
