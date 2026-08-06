package com.cybrisoft.redstoneeda.networking.C2S;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.networking.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record C2SEditProjectPacket(Project project) implements CustomPayload {
    public static final Id<C2SEditProjectPacket> ID = new Id<>(NetworkingConstants.C2S_EDIT_PROJECT);
    public static final PacketCodec<RegistryByteBuf, C2SEditProjectPacket> CODEC = PacketCodec.tuple(
            Project.PACKET_CODEC, C2SEditProjectPacket::project,
            C2SEditProjectPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
