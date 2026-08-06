package com.cybrisoft.redstoneeda.breakpoints;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.world.World;

public interface BreakpointCondition {
    BreakpointResult evaluate(World world);

    String getName();

    public static final PacketCodec<PacketByteBuf, BreakpointCondition> PACKET_CODEC = new PacketCodec<PacketByteBuf, BreakpointCondition>() {
        @Override
        public void encode(PacketByteBuf buf, BreakpointCondition condition) {
            if (condition instanceof BinaryCondition c) {
                buf.writeInt(0);
                BinaryCondition.PACKET_CODEC.encode(buf, c);
            } else if (condition instanceof BlockstateCondition c) {
                buf.writeInt(1);
                BlockstateCondition.PACKET_CODEC.encode(buf, c);
            }
        }

        @Override
        public BreakpointCondition decode(PacketByteBuf buf) {
            int type = buf.readInt();
            if (type==0) {
                return BinaryCondition.PACKET_CODEC.decode(buf);
            } else if (type==1) {
                return BlockstateCondition.PACKET_CODEC.decode(buf);
            }
            return null;
        }
    };
}
