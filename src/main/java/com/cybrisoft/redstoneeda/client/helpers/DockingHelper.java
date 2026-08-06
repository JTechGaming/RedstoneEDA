package com.cybrisoft.redstoneeda.client.helpers;

import com.cybrisoft.redstoneeda.Redstoneeda;
import com.cybrisoft.redstoneeda.client.uiElements.windows.*;
import com.cybrisoft.redstoneeda.client.util.IniUtil;
import imgui.ImGui;
import imgui.ImGuiIO;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Environment(EnvType.CLIENT)
public class DockingHelper {
    private static boolean stackTraceDocked = false;
    private static boolean variableTrackerDocked = false;

    private static boolean breakpointShouldSwap = false;
    private static boolean debuggerShouldSwap = false;
    private static boolean preferencesShouldSwap = false;
    private static boolean projectShouldSwap = false;
    private static boolean stackTraceShouldSwap = false;
    private static boolean variableTrackerShouldSwap = false;

    public static void render() {
        // update visibility states
        if (breakpointShouldSwap) {
            BreakpointWindow.isOpen.set(!BreakpointWindow.isOpen.get());
        }
        if (debuggerShouldSwap) {
            BreakpointWindow.isOpen.set(!BreakpointWindow.isOpen.get());
        }
        if (preferencesShouldSwap) {
            BreakpointWindow.isOpen.set(!BreakpointWindow.isOpen.get());
        }
        if (projectShouldSwap) {
            BreakpointWindow.isOpen.set(!BreakpointWindow.isOpen.get());
        }
        if (stackTraceShouldSwap) {
            BreakpointWindow.isOpen.set(stackTraceDocked || !BreakpointWindow.isOpen.get());
        }
        if (variableTrackerShouldSwap) {
            BreakpointWindow.isOpen.set(variableTrackerDocked || !BreakpointWindow.isOpen.get());
        }

        if (stackTraceDocked && !BreakpointWindow.isOpen.get()) {
            stackTraceDocked = false;
        }
        if (variableTrackerDocked && !BreakpointWindow.isOpen.get()) {
            variableTrackerDocked = false;
        }

        // draw the windows
        PreferencesWindow.render();
        BreakpointWindow.render();
        DebuggerWindow.render();
        StackTraceWindow.render();
        ProjectWindow.render();
    }

    public static boolean isStackTraceDocked() {
        return stackTraceDocked;
    }

    public static void setStackTraceDocked(boolean stackTraceDocked) {
        DockingHelper.stackTraceDocked = stackTraceDocked;
    }

    public static boolean isVariableTrackerDocked() {
        return variableTrackerDocked;
    }

    public static void setVariableTrackerDocked(boolean variableTrackerDocked) {
        DockingHelper.variableTrackerDocked = variableTrackerDocked;
    }



    public static boolean isBreakpointShouldSwap() {
        return breakpointShouldSwap;
    }

    public static void setBreakpointShouldSwap(boolean breakpointShouldSwap) {
        DockingHelper.breakpointShouldSwap = breakpointShouldSwap;
    }

    public static boolean isDebuggerShouldSwap() {
        return debuggerShouldSwap;
    }

    public static void setDebuggerShouldSwap(boolean debuggerShouldSwap) {
        DockingHelper.debuggerShouldSwap = debuggerShouldSwap;
    }

    public static boolean isPreferencesShouldSwap() {
        return preferencesShouldSwap;
    }

    public static void setPreferencesShouldSwap(boolean preferencesShouldSwap) {
        DockingHelper.preferencesShouldSwap = preferencesShouldSwap;
    }

    public static boolean isProjectShouldSwap() {
        return projectShouldSwap;
    }

    public static void setProjectShouldSwap(boolean projectShouldSwap) {
        DockingHelper.projectShouldSwap = projectShouldSwap;
    }

    public static boolean isStackTraceShouldSwap() {
        return stackTraceShouldSwap;
    }

    public static void setStackTraceShouldSwap(boolean stackTraceShouldSwap) {
        DockingHelper.stackTraceShouldSwap = stackTraceShouldSwap;
    }

    public static boolean isVariableTrackerShouldSwap() {
        return variableTrackerShouldSwap;
    }

    public static void setVariableTrackerShouldSwap(boolean variableTrackerShouldSwap) {
        DockingHelper.variableTrackerShouldSwap = variableTrackerShouldSwap;
    }
}
