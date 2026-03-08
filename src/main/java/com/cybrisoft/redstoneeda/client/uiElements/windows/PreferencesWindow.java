package com.cybrisoft.redstoneeda.client.uiElements.windows;

import com.cybrisoft.redstoneeda.client.config.ModConfig;
import com.cybrisoft.redstoneeda.client.helpers.DisplayScaleHelper;
import com.cybrisoft.redstoneeda.client.imgui.ImGuiImplementation;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.Map;

@Environment(EnvType.CLIENT)
public class PreferencesWindow {
    public static ImInt fontSize = new ImInt(ModConfig.getInt("fontsize", DisplayScaleHelper.getIdealFontSize()));
    public static ImInt selectedFont = new ImInt(ModConfig.getInt("font",
            ImGuiImplementation.loadedFontNames.indexOf("Roboto (Regular)")
    ));

    public static ImBoolean isOpen = new ImBoolean(false);

    public static void render() {
        if (!isOpen.get()) {
            return; // If the window is not open, do not render
        }

        // Set position to center of viewport
        ImVec2 centerPos = ImGuiImplementation.getCenterViewportPos();
        ImGui.setNextWindowPos(centerPos.x, centerPos.y, ImGuiCond.Always, 0.5f, 0.5f);

        if (ImGui.begin("Settings", isOpen)) {
            if (ImGui.collapsingHeader("General Settings")) {

            }
            if (ImGui.collapsingHeader("Appearance & Behavior")) {
                if (ImGui.inputInt("Font size", fontSize)) {
                    ImGui.getIO().setFontGlobalScale(fontSize.get() / 14.0f);
                    ModConfig.updateSettings(Map.of("fontsize", fontSize.get()));
                }
                if (ImGui.combo("Font", selectedFont, ImGuiImplementation.loadedFontNames.toArray(String[]::new))) {
                    ImGuiImplementation.currentFont = ImGuiImplementation.loadedFonts.get(selectedFont.get());
                    ModConfig.updateSettings(Map.of("font", selectedFont.get()));
                }
            }

            // Close button in bottom right
            ImGui.setCursorPosY(ImGui.getWindowHeight() - ImGui.getFrameHeightWithSpacing());
            ImGui.setCursorPosX(ImGui.getWindowWidth() - ImGui.getFrameHeightWithSpacing() - ImGui.getStyle().getItemSpacingX() - ImGui.getStyle().getWindowMinSizeX());
            if (ImGui.button("Close")) {
                isOpen.set(false); // Close the settings window
            }
        }
        ImGui.end();
    }
}
