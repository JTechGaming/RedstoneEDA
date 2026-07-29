package com.cybrisoft.redstoneeda;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Redstoneeda implements ModInitializer {
    public static final String MOD_ID = "redstoneeda";
    public static Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final String version = "1.0.0-1.21.11+d152";

    public static boolean debugMode = false;

    public static Identifier identifier(String name) {
        return Identifier.of(MOD_ID, name);
    }

    @Override
    public void onInitialize() {
    }
}
