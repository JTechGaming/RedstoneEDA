package com.cybrisoft.redstoneeda.client.uiElements.windows;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiKeyModFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class DebuggerWindow {
    public static ImBoolean isOpen = new ImBoolean(true);

    public static void render() {
        if (!isOpen.get()) {
            return; // If the window is not open, do not render
        }

        if (ImGui.begin("Debugger", isOpen, ImGuiWindowFlags.MenuBar)) {
            if (ImGui.beginMenuBar()) {
                if (ImGui.menuItem("Temp", null)) {

                }
                ImGui.endMenuBar();
            }

            if (ImGui.beginChild("debuggerplayback")) {
                drawGrid();
            }
            ImGui.endChild();
            if (ImGui.isItemHovered()) {
                float wheel = ImGui.getIO().getMouseWheel();

                if (ImGui.getIO().getKeyCtrl()) {
                    zoom = (float) Math.clamp(zoom + wheel * 0.5f, 0.3, 5);
                } else {
                    scrollX = Math.max(scrollX + wheel * 10 / zoom, 0);
                }
                draggingPlayhead = true;
            }
        }
        ImGui.end();
    }

    private static float scrollX = 0.0f;
    private static float zoom = 1.0f; // 1 is normal
    private static int lines = 25;
    private static final int categories = 5;
    private static int selectedTick = 1;

    private static final float playheadThickness = 10f;

    private static boolean draggingPlayhead = false;

    private static void drawGrid() {
        ImVec2 startP = ImGui.getItemRectMin();
        ImVec2 p = ImGui.getItemRectMin();

        float itemWidth = ImGui.getContentRegionAvailX();
        float width = itemWidth * 0.95f;
        float height = ImGui.getContentRegionAvailY() - 60;

        p.plus(itemWidth - width - 20, 20);

        float lineDistance = (width / (lines-1));

        ImDrawList drawList = ImGui.getWindowDrawList();

        // horizontal lines
        for (int i=0; i<=categories; i++) {
            float y = p.y + (height / categories) * i;
            drawList.addLine(p.x, y, p.x + width, y, 0xFFFFFFFF);

            String labelText = "Category" + i;
            drawList.addText(startP.x, y - ImGui.calcTextSize(labelText).y/2, 0xFFFFFFFF, labelText);
        }

        // vertical lines
        int ticksOnScreen = Math.min((int) Math.floor(width / lineDistance), lines);
        float xOffset = (width - ticksOnScreen * lineDistance) / 2;
        int firstTick = (int) Math.floor(scrollX / lineDistance);
        for (int i=firstTick; i<=firstTick+ticksOnScreen; i++) {
            float x = xOffset + p.x + lineDistance * i - scrollX;
            drawList.addLine(x, p.y, x, p.y + height, 0xFFFFFFFF);

            drawList.addText(x - ImGui.calcTextSize(i + "").x/2, p.y + height + 10, 0xFFFFFFFF, i + "");

            if (i == selectedTick) continue;

            ImGui.setCursorScreenPos(x - lineDistance/4, p.y);
            ImGui.dummy(lineDistance/2 , height);
            //drawList.addRectFilled(x - lineDistance/4, p.y, x + lineDistance/4, p.y + height, 0xFFF00FFF);
            if (ImGui.isItemClicked()) {
                selectedTick = i;
                //System.out.println("Line: " + i);
            }
        }

        // Playhead
        float x = xOffset + p.x + lineDistance * selectedTick - scrollX;
        drawList.addLine(x, p.y, x, p.y + height + 10, 0xFFFFFFFF, playheadThickness);
        drawList.addCircleFilled(x, p.y, 20, 0xFFFFFFFF);

        ImGui.setCursorScreenPos(x - playheadThickness/4, p.y);
        ImGui.dummy(playheadThickness , height);

        if (ImGui.isMouseDragging(0)) {
            if (!draggingPlayhead) return;
            ImVec2 mousePos = ImGui.getMousePos();

            int i = Math.round((mousePos.x - xOffset - p.x + scrollX) / lineDistance);
            if (i>=0 && i<lines) {
                selectedTick = i;
            }
        } else {
            draggingPlayhead = false;
        }
    }
}
