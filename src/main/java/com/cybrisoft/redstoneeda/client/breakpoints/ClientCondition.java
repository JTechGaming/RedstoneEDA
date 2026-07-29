package com.cybrisoft.redstoneeda.client.breakpoints;

import net.minecraft.world.World;

public interface ClientCondition {
    boolean evaluate(World world);

    String getName();
}
