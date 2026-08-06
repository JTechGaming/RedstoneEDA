package com.cybrisoft.redstoneeda.client.uiElements.windows;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.client.RedstoneedaClient;
import com.cybrisoft.redstoneeda.client.helpers.Docks;
import com.cybrisoft.redstoneeda.client.imgui.ImGuiImplementation;
import com.cybrisoft.redstoneeda.client.util.IniUtil;
import com.cybrisoft.redstoneeda.client.util.Selection;
import com.cybrisoft.redstoneeda.networking.C2S.C2SEditProjectPacket;
import com.cybrisoft.redstoneeda.networking.C2S.C2SInfoPacket;
import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.UUID;

@Environment(EnvType.CLIENT)
public class ProjectWindow {
    public static ImBoolean isOpen = new ImBoolean(true);

    private static ImString namebox = new ImString();
    private static boolean lastProjectNull = false;
    private static boolean openingProject = false;

    private static boolean testSwitchIni = false;

    public static void render() {
        if (!isOpen.get()) {
            return; // If the window is not open, do not render
        }

        if (ImGui.begin("Project", isOpen)) {
            if (openingProject) {
                if (!RedstoneedaClient.availableProjects.isEmpty()) {
                    for (UUID uuid : RedstoneedaClient.availableProjects.keySet()) {
                        String name = RedstoneedaClient.availableProjects.get(uuid);

                        if (ImGui.selectable(name + "###" + uuid.toString())) {
                            ClientPlayNetworking.send(new C2SInfoPacket(C2SInfoPacket.Ops.REQUEST.id(), uuid.toString()));
                        }
                        ImGui.textDisabled(uuid.toString());
                    }
                } else {
                    ImGuiImplementation.centeredText("No projects yet");
                    if (ImGui.button("Create Project")) {
                        openingProject = false;

                        Project project = new Project(
                                UUID.randomUUID(),
                                "New Project",
                                MinecraftClient.getInstance().player.getEntityWorld().getRegistryKey(),
                                new ArrayList<>()
                        );

                        RedstoneedaClient.setClientProject(project);
                        ClientPlayNetworking.send(new C2SEditProjectPacket(project));
                    }
                }

                if (RedstoneedaClient.getClientProject() != null) openingProject = false;

                ImGui.end();
                return;
            }

            Project project = RedstoneedaClient.getClientProject();
            if (project != null) {
                if (lastProjectNull) {
                    namebox.set(project.getName());
                    lastProjectNull = false;
                }
                ImGui.text("Name: ");
                ImGui.sameLine();
                if (ImGui.inputText("##projectnameinput", namebox)) {
                    project.setName(namebox.get());
                    ClientPlayNetworking.send(new C2SEditProjectPacket(project));
                }

                ImGui.spacing();

                ImGui.text("World: ");
                ImGui.sameLine();
                ImGui.text(project.getWorld().getValue().getPath());

                ImGui.spacing();

                ImGui.text("Project Area: ");
                if (project.getMin() == null) {
                    ImGui.text("No area selected");
                    ImGui.spacing();
                    if (ImGui.button("Create Selection")) {
                        RedstoneedaClient.activeSelections.add(new Selection((pos1, pos2) -> {
                            project.setMin(pos1);
                            project.setMax(pos2);
                            ClientPlayNetworking.send(new C2SEditProjectPacket(project));
                        }));
                    }
                } else {
                    ImGui.text("Min: ");
                    ImGui.sameLine();
                    ImGui.text(project.getMin().toShortString());

                    ImGui.text("Min: ");
                    ImGui.sameLine();
                    ImGui.text(project.getMax().toShortString());

                    ImGui.spacing();

                    if (ImGui.button("Edit/View Selection")) {
                        RedstoneedaClient.activeSelections.add(new Selection(project.getMin(), project.getMax(), (pos1, pos2) -> {
                            project.setMin(pos1);
                            project.setMax(pos2);
                            ClientPlayNetworking.send(new C2SEditProjectPacket(project));
                        }));
                    }
                }

                ImGui.spacing();
                ImGui.separator();
                ImGui.spacing();

                if (ImGui.button("Freeze test")) {
                    ClientPlayNetworking.send(new C2SInfoPacket(C2SInfoPacket.Ops.TOGGLE_FREEZE.id(), project.getUuid().toString()));
                }

                boolean lastIni = testSwitchIni;

                if (ImGui.button("Switch ini test")) {
                    testSwitchIni = !testSwitchIni;
                }

                if (lastIni != testSwitchIni) {
                    if (testSwitchIni) {
                        IniUtil.scheduleIniLoad(Docks.DEBUGGER.get());
                        System.out.println("debugger");
                    } else {
                        IniUtil.scheduleIniLoad(Docks.DEFAULT.get());
                        System.out.println("default");
                    }
                }
            } else {
                lastProjectNull = true;
                ImGui.text("No project opened");
                if (ImGui.button("Open a project")) {
                    openingProject = true;
                    RedstoneedaClient.availableProjects.clear();
                    ClientPlayNetworking.send(new C2SInfoPacket(C2SInfoPacket.Ops.QUERY.id(), ""));
                }
            }
        }
        ImGui.end();
    }

    public static void setOpeningProject(boolean openingProject) {
        ProjectWindow.openingProject = openingProject;
    }
}
