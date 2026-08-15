package com.cybrisoft.redstoneeda.util;

import net.minecraft.util.math.BlockPos;

public class MathUtils {
    public static boolean intersects(BlockPos pos, BlockPos p1, BlockPos p2) {
        return  pos.getX() >= Math.min(p1.getX(), p2.getX()) &&
                pos.getX() <= Math.max(p1.getX(), p2.getX()) &&
                pos.getY() >= Math.min(p1.getY(), p2.getY()) &&
                pos.getY() <= Math.max(p1.getY(), p2.getY()) &&
                pos.getZ() >= Math.min(p1.getZ(), p2.getZ()) &&
                pos.getZ() <= Math.max(p1.getZ(), p2.getZ());
    }
}
