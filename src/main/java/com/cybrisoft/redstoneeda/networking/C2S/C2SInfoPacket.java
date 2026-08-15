package com.cybrisoft.redstoneeda.networking.C2S;

import com.cybrisoft.redstoneeda.networking.NetworkingConstants;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record C2SInfoPacket(String op, String data) implements CustomPayload {
    public static final Id<C2SInfoPacket> ID = new Id<>(NetworkingConstants.C2S_INFO);
    public static final PacketCodec<RegistryByteBuf, C2SInfoPacket> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, C2SInfoPacket::op,
            PacketCodecs.STRING, C2SInfoPacket::data,
            C2SInfoPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public enum Ops {
        QUERY("query_server_projects"),
        REQUEST("query_request_project"),
        CLEAR("clear_project"),
        TOGGLE_FREEZE("toggle_project_freeze"),
        TOGGLE_DEBUG("toggle_project_debug")
        ;

        String identifier;

        public String id() {
            return identifier;
        }

        Ops(String identifier) {
            this.identifier = identifier;
        }
    }
}
