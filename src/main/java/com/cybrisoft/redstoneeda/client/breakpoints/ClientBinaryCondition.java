package com.cybrisoft.redstoneeda.client.breakpoints;

import com.cybrisoft.redstoneeda.client.util.Endianness;
import net.minecraft.block.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.concurrent.atomic.AtomicInteger;

public class ClientBinaryCondition implements ClientCondition {
    private BlockPos pos1 = new BlockPos(0, 0, 0);
    private BlockPos pos2 = new BlockPos(0, 0, 0);
    private Endianness endianness = Endianness.BIG;
    private int desiredValue = 0;

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
    public boolean evaluate(World world) {
        AtomicInteger bit = new AtomicInteger(1);
        AtomicInteger value = new AtomicInteger();

        BlockPos.iterate(pos1, pos2).forEach((pos) -> {
            BlockState bs = world.getBlockState(pos);
            if (!isPowerBlock(bs)) return;

            value.addAndGet(bit.get() * (readOnState(bs) ? 1 : 0));

            bit.updateAndGet(v -> v * 2);
        });

        return value.get() == desiredValue;
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
}
