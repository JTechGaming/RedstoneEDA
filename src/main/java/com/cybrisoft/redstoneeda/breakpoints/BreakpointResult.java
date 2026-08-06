package com.cybrisoft.redstoneeda.breakpoints;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record BreakpointResult(
        ConditionTypes type,
        UUID breakpoint,
        BreakpointSelection selection,
        BlockPos[] positions,
        int tick
) {
    public static final PacketCodec<PacketByteBuf, BreakpointResult> PACKET_CODEC = new PacketCodec<PacketByteBuf, BreakpointResult>() {
        @Override
        public void encode(PacketByteBuf buf, BreakpointResult result) {
            buf.writeString(result.type().name);
            buf.writeUuid(result.breakpoint());
            buf.writeBoolean(result.selection != null);
            if (result.selection != null) {
                buf.writeBlockPos(result.selection.pos1());
                buf.writeBlockPos(result.selection.pos2());
            }
            if (result.positions == null) {
                buf.writeInt(0);
            } else {
                buf.writeInt(result.positions.length);
                for (BlockPos pos : result.positions) {
                    buf.writeBlockPos(pos);
                }
            }
            buf.writeInt(result.tick);
        }

        @Override
        public BreakpointResult decode(PacketByteBuf buf) {
            ConditionTypes type = ConditionTypes.fromName(buf.readString());
            UUID breakpoint = buf.readUuid();
            BreakpointSelection selection = null;
            if (buf.readBoolean()) {
                selection = new BreakpointSelection(buf.readBlockPos(), buf.readBlockPos());
            }
            int posCount = buf.readInt();
            BlockPos[] positions = new BlockPos[posCount];
            for (int i=0; i<posCount; i++) {
                positions[i] = buf.readBlockPos();
            }
            int tick = buf.readInt();

            return new BreakpointResult(type, breakpoint, selection, positions, tick);
        }
    };
} //todo add line and more info
