package com.cybrisoft.redstoneeda.breakpoints;

import net.minecraft.util.math.BlockPos;

public record BreakpointSelection(
        BlockPos pos1,
        BlockPos pos2
) {  }
