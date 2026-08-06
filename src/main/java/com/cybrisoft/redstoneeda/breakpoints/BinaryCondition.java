package com.cybrisoft.redstoneeda.breakpoints;

import com.cybrisoft.redstoneeda.client.util.Endianness;
import net.minecraft.block.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class BinaryCondition implements BreakpointCondition {
    public UUID uuid = null;
    private BlockPos pos1 = new BlockPos(0, 0, 0);
    private BlockPos pos2 = new BlockPos(0, 0, 0);
    private Endianness endianness = Endianness.BIG;
    private int desiredValue = 0;

    public BinaryCondition(UUID uuid) {
        this.uuid = uuid;
    }

    public BinaryCondition(UUID uuid, BlockPos pos1, BlockPos pos2, Endianness endianness, int desiredValue) {
        this.uuid = uuid;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.endianness = endianness;
        this.desiredValue = desiredValue;
    }

    private boolean isPowerBlock(BlockState bs) {
        return  bs.getBlock().equals(Blocks.REDSTONE_WIRE) || bs.getBlock().equals(Blocks.REDSTONE_BLOCK) ||
                bs.getBlock().equals(Blocks.LEVER) || bs.getBlock().equals(Blocks.REDSTONE_LAMP) ||
                bs.getBlock().equals(Blocks.REPEATER) || bs.getBlock().equals(Blocks.COMPARATOR);
    }

    private boolean readOnState(BlockState bs) {
        if (bs.getBlock().equals(Blocks.REDSTONE_WIRE)) {
            return bs.get(RedstoneWireBlock.POWER) > 0;
        } else if (bs.getBlock().equals(Blocks.REDSTONE_BLOCK)) { return true; }
        else if (bs.getBlock().equals(Blocks.LEVER)) {
            return bs.get(LeverBlock.POWERED);
        }
        else if (bs.getBlock().equals(Blocks.REDSTONE_LAMP)) {
            return bs.get(RedstoneLampBlock.LIT);
        }
        else if (bs.getBlock().equals(Blocks.REPEATER)) {
            return bs.get(RepeaterBlock.POWERED);
        }
        else if (bs.getBlock().equals(Blocks.COMPARATOR)) {
            return bs.get(ComparatorBlock.POWERED);
        }
        return false;
    }

    @Override
    public BreakpointResult evaluate(World world) {
        AtomicInteger bit = new AtomicInteger(1);
        AtomicInteger value = new AtomicInteger();

        BlockPos.iterate(pos1, pos2).forEach((pos) -> {
            BlockState bs = world.getBlockState(pos);
            if (!isPowerBlock(bs)) return;

            value.addAndGet(bit.get() * (readOnState(bs) ? 1 : 0));

            bit.updateAndGet(v -> v * 2);
        });

        return value.get() == desiredValue ? new BreakpointResult(ConditionTypes.BINARY, uuid, new BreakpointSelection(pos1, pos2), null, 0) : null;
    }

    @Override
    public String getName() {
        return "Binary Condition";
    }

    public BlockPos getPos1() {
        return pos1;
    }

    public BlockPos getPos2() {
        return pos2;
    }

    public Endianness getEndianness() {
        return endianness;
    }

    public void setDesiredValue(int desiredValue) {
        this.desiredValue = desiredValue;
    }

    public void setEndianness(Endianness endianness) {
        this.endianness = endianness;
    }

    public int getDesiredValue() {
        return desiredValue;
    }

    public static final PacketCodec<PacketByteBuf, BinaryCondition> PACKET_CODEC = new PacketCodec<PacketByteBuf, BinaryCondition>() {
        @Override
        public void encode(PacketByteBuf buf, BinaryCondition value) {
            buf.writeUuid(value.uuid);
            BlockPos.PACKET_CODEC.encode(buf, value.getPos1());
            BlockPos.PACKET_CODEC.encode(buf, value.getPos2());
            buf.writeInt(value.getEndianness().ordinal());
            buf.writeInt(value.getDesiredValue());
        }

        @Override
        public BinaryCondition decode(PacketByteBuf buf) {
            return new BinaryCondition(buf.readUuid(), BlockPos.PACKET_CODEC.decode(buf), BlockPos.PACKET_CODEC.decode(buf), Endianness.values()[buf.readInt()], buf.readInt());
        }
    };
}
