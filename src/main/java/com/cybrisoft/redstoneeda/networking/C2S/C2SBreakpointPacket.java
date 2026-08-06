package com.cybrisoft.redstoneeda.networking.C2S;

import com.cybrisoft.redstoneeda.breakpoints.Breakpoint;
import com.cybrisoft.redstoneeda.networking.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record C2SBreakpointPacket(Breakpoint bp) implements CustomPayload {
    public static final Id<C2SBreakpointPacket> ID = new Id<>(NetworkingConstants.C2S_BREAKPOINTS);
    public static final PacketCodec<RegistryByteBuf, C2SBreakpointPacket> CODEC = PacketCodec.tuple(
            Breakpoint.PACKET_CODEC, C2SBreakpointPacket::bp,
            C2SBreakpointPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
