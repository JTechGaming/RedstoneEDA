package com.cybrisoft.redstoneeda.networking.S2C;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.breakpoints.BreakpointResult;
import com.cybrisoft.redstoneeda.networking.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record S2CTriggeredBreakpointPacket(BreakpointResult result) implements CustomPayload {
    public static final Id<S2CTriggeredBreakpointPacket> ID = new Id<>(NetworkingConstants.S2C_BREAKPOINT_TRIGGERED);
    public static final PacketCodec<RegistryByteBuf, S2CTriggeredBreakpointPacket> CODEC = PacketCodec.tuple(
            BreakpointResult.PACKET_CODEC, S2CTriggeredBreakpointPacket::result,
            S2CTriggeredBreakpointPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
