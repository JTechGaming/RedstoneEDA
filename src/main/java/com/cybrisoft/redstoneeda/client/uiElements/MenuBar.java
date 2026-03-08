package com.cybrisoft.redstoneeda.client.uiElements;

import com.cybrisoft.redstoneeda.client.uiElements.windows.PreferencesWindow;
import imgui.ImGui;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class MenuBar {
    private static List<ResourcePackProfile> packs = new ArrayList<>();
    private static boolean firstOpenPack = true;
    private static boolean firstOpenInternalPack = true;

    public static void render() {
        if (ImGui.beginMainMenuBar()) {
            if (ImGui.beginMenu("File")) {
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Edit")) {
                if (ImGui.menuItem("Preferences", null, PreferencesWindow.isOpen.get())) {
                    PreferencesWindow.isOpen.set(!PreferencesWindow.isOpen.get());
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Window")) {
                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }
    }

}
