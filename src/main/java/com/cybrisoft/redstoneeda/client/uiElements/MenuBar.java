package com.cybrisoft.redstoneeda.client.uiElements;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.client.RedstoneedaClient;
import com.cybrisoft.redstoneeda.client.helpers.DockingHelper;
import com.cybrisoft.redstoneeda.client.helpers.Docks;
import com.cybrisoft.redstoneeda.client.uiElements.windows.PreferencesWindow;
import com.cybrisoft.redstoneeda.client.uiElements.windows.ProjectWindow;
import com.cybrisoft.redstoneeda.networking.C2S.C2SEditProjectPacket;
import com.cybrisoft.redstoneeda.networking.C2S.C2SInfoPacket;
import imgui.ImGui;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackProfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Environment(EnvType.CLIENT)
public class MenuBar {
    public static void render() {
        if (MinecraftClient.getInstance().player == null) return;
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
                if (ImGui.menuItem("Stacktrace")) {
                    DockingHelper.setStackTraceShouldSwap(true);
                    DockingHelper.setStackTraceDocked(!DockingHelper.isStackTraceDocked());
                }
                if (ImGui.menuItem("Variable Tracker")) {
                    DockingHelper.setVariableTrackerShouldSwap(true);
                    DockingHelper.setVariableTrackerDocked(!DockingHelper.isVariableTrackerDocked());
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Project")) {
                if (ImGui.menuItem("New", null)) {
                    Project project = new Project(
                            UUID.randomUUID(),
                            "New Project",
                            MinecraftClient.getInstance().player.getEntityWorld().getRegistryKey(),
                            new ArrayList<>()
                    );

                    RedstoneedaClient.setClientProject(project);
                    ClientPlayNetworking.send(new C2SEditProjectPacket(project));
                }

                if (ImGui.menuItem("Open", null)) {
                    if (RedstoneedaClient.getClientProject() != null) {
                        ClientPlayNetworking.send(new C2SEditProjectPacket(RedstoneedaClient.getClientProject())); // auto save
                    }
                    RedstoneedaClient.setClientProject(null);
                    ClientPlayNetworking.send(new C2SInfoPacket(C2SInfoPacket.Ops.CLEAR.id(), ""));
                    ProjectWindow.setOpeningProject(true);
                    RedstoneedaClient.availableProjects.clear();
                    ClientPlayNetworking.send(new C2SInfoPacket(C2SInfoPacket.Ops.QUERY.id(), ""));
                }

                if (RedstoneedaClient.getClientProject() != null) {
                    ImGui.separator();
                    if (ImGui.menuItem("Close", null)) {
                        ClientPlayNetworking.send(new C2SEditProjectPacket(RedstoneedaClient.getClientProject())); // auto save
                        RedstoneedaClient.setClientProject(null);
                        ClientPlayNetworking.send(new C2SInfoPacket(C2SInfoPacket.Ops.CLEAR.id(), ""));
                    }
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu("Format")) {
                if (ImGui.menuItem("Fix Illegal Dust States", null)) {

                }
                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }
    }
}
