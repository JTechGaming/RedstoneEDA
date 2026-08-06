package com.cybrisoft.redstoneeda.client.util;

import com.cybrisoft.redstoneeda.Redstoneeda;
import imgui.ImGui;
import imgui.ImGuiIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Environment(EnvType.CLIENT)
public class IniUtil {
    private static String defaultIni = """
        [Window][DockHost]
        Pos=0,0
        Size=3840,2161
        Collapsed=0
        
        [Window][Project]
        Pos=4,28
        Size=621,1458
        Collapsed=0
        DockId=0x00000001,0
        
        [Window][Debug##Default]
        Pos=60,60
        Size=400,400
        Collapsed=0
        
        [Window][Breakpoints]
        Pos=3154,28
        Size=676,2122
        Collapsed=0
        DockId=0x00000006,0
        
        [Window][Debugger]
        Pos=4,1488
        Size=3148,662
        Collapsed=0
        DockId=0x00000004,0
        
        [Docking][Data]
        DockSpace       ID=0x1FF6FA18 Window=0x9BD87705 Pos=4,28 Size=3826,2122 Split=X Selected=0x5A3E6375
          DockNode      ID=0x00000005 Parent=0x1FF6FA18 SizeRef=3148,2122 Split=Y
            DockNode    ID=0x00000003 Parent=0x00000005 SizeRef=3826,1458 Split=X
              DockNode  ID=0x00000001 Parent=0x00000003 SizeRef=621,2122 Selected=0xE00EE972
              DockNode  ID=0x00000002 Parent=0x00000003 SizeRef=2525,2122 CentralNode=1 Selected=0x5A3E6375
            DockNode    ID=0x00000004 Parent=0x00000005 SizeRef=3826,662 Selected=0x289D2C3F
          DockNode      ID=0x00000006 Parent=0x1FF6FA18 SizeRef=676,2122 Selected=0x0263173C""";

    private static String scheduledIni = null;

    public static String getScheduledIni() {
        return scheduledIni;
    }

    public static void scheduleIniLoad(String dockName) {
        scheduledIni = dockName;
    }

    public static void loadIni(String dockName) {
        Path iniPath = MinecraftClient.getInstance().runDirectory.toPath().resolve("config/" + Redstoneeda.MOD_ID + "/" + (dockName + ".ini"));
        File iniFile = iniPath.toFile();
        boolean exists = iniFile.exists();
        if (!exists) {
            try {
                iniFile.getParentFile().mkdirs();
                iniFile.createNewFile();
                Files.write(iniFile.toPath(), IniUtil.defaultIni.getBytes());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }

        final ImGuiIO data = ImGui.getIO();
        data.setIniFilename(iniPath.toString());

        scheduledIni = null;
    }
}