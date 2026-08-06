package com.cybrisoft.redstoneeda.client.uiElements.windows;

import com.cybrisoft.redstoneeda.managers.ClientStackTraceHandler;
import com.cybrisoft.redstoneeda.managers.Trace;
import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class StackTraceWindow {
    public static ImBoolean isOpen = new ImBoolean(false);

    public static void render() {
        if (!isOpen.get()) {
            return; // If the window is not open, do not render
        }

        if (ImGui.begin("Stacktrace", isOpen, ImGuiWindowFlags.MenuBar)) {
            if (ImGui.beginMenuBar()) {
                if (ImGui.menuItem("Temp", null)) {

                }
                ImGui.endMenuBar();
            }

            for (int i = 0; i< ClientStackTraceHandler.getAll().size(); i++) {
                Trace trace = ClientStackTraceHandler.getAll().get(i);
                ImGui.text(trace.result().selection().pos1().toShortString());
                ImGui.sameLine();
                ImGui.text(trace.result().selection().pos2().toShortString());
            }
        }
        ImGui.end();
    }
}
