package com.cybrisoft.redstoneeda.networking;

import com.cybrisoft.redstoneeda.Redstoneeda;
import net.minecraft.util.Identifier;

public class NetworkingConstants {
    /**
     * C2S
     */
    public static final Identifier C2S_BREAKPOINTS = Identifier.of(Redstoneeda.MOD_ID, "c2s_breakpoints");
    public static final Identifier C2S_INFO = Identifier.of(Redstoneeda.MOD_ID, "c2s_info");
    public static final Identifier C2S_EDIT_PROJECT = Identifier.of(Redstoneeda.MOD_ID, "c2s_edit_project");
    public static final Identifier C2S_OPEN_PROJECT = Identifier.of(Redstoneeda.MOD_ID, "c2s_open_project");

    /**
     * S2C
     */
    public static final Identifier S2C_SYNC_PROJECT = Identifier.of(Redstoneeda.MOD_ID, "s2c_sync_project");
    public static final Identifier S2C_QUERY_PROJECTS = Identifier.of(Redstoneeda.MOD_ID, "s2c_query_projects");
    public static final Identifier S2C_BREAKPOINT_TRIGGERED = Identifier.of(Redstoneeda.MOD_ID, "s2c_breakpoint_triggered");
}
