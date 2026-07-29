package com.cybrisoft.redstoneeda.client.breakpoints;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class ClientBlockstateCondition implements ClientCondition {
    public List<BlockState> expectedStates = new ArrayList<>();
    public List<int[]> expectedPositions = new ArrayList<>();

    @Override
    public boolean evaluate(World world) {
        if (expectedPositions.size() != expectedStates.size()) {
            throw new IllegalArgumentException("ClientBlockstateCondition: expected positions and expected state counts misaligned!");
        }
        for (int i=0; i<expectedPositions.size(); i++) {
            int[] coords = expectedPositions.get(i);
            if (!world.getBlockState(new BlockPos(coords[0], coords[1], coords[2])).equals(expectedStates.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return "Blockstate condition";
    }
}
