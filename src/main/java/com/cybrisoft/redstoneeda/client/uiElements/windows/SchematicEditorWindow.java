package com.cybrisoft.redstoneeda.client.uiElements.windows;

import imgui.ImDrawList;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImString;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchematicEditorWindow {
    private static final List<Component> componentList = new ArrayList<>();
    private static final List<Pair<Integer, Integer>> links = new ArrayList<>();
    private static final Map<Integer, Pin> pinLookup = new HashMap<>();
    private static int currentID = 0;

    private static final float GRID_SIZE = 16f;

    private static final ImVec2 scrolling = new ImVec2(0, 0);
    private static float zoom = 1.0f;

    private static Component selectedNode = null;
    private static Pin activePin = null;
    private static boolean rerouteCreatedThisFrame = false;

    public static boolean addComponentPopupOpen = false;

    private static ImString searchQueryBuffer = new ImString(128);
    private static String searchQuery = "";
    private static int selectedIndex = 0;
    private static final int MAX_RESULTS = 20;

    private static final List<String> allComponents = List.of(
            "Add",
            "Multiply",
            "Divide",
            "Clamp",
            "Sine",
            "Cosine",
            "Texture Sample",
            "Constant",
            "Vector3",
            "Vector4"
    );

    static {
        componentList.add(new Component("Test Node A", new ArrayList<>(List.of(
                new Pin("Out", PinType.OUTPUT), new Pin("In", PinType.INPUT))), 50, 50));
        componentList.add(new Component("Adder", new ArrayList<>(List.of(
                new Pin("Out", PinType.OUTPUT), new Pin("InA", PinType.INPUT), new Pin("InB", PinType.INPUT), new Pin("Clock", PinType.INPUT))), 300, 150));
        componentList.add(new Component("4-bit adder", new ArrayList<>(List.of(
                new Pin("1", PinType.INPUT), new Pin("2", PinType.INPUT), new Pin("4", PinType.INPUT), new Pin("8", PinType.INPUT), new Pin("Carry-In", PinType.INPUT),
                new Pin("1", PinType.OUTPUT), new Pin("2", PinType.OUTPUT), new Pin("4", PinType.OUTPUT), new Pin("8", PinType.OUTPUT), new Pin("Carry-Out", PinType.OUTPUT))), 300, 150));
        componentList.add(new Component("Adder", new ArrayList<>(List.of(
                new Pin("TestToSeeIfTheXSizeActuallyResizesProperly", PinType.OUTPUT), new Pin("InA", PinType.INPUT), new Pin("InB", PinType.INPUT), new Pin("Clockasdadsadadasasdasd", PinType.INPUT))), 300, 150));
    }

    public static void render() {
        rerouteCreatedThisFrame = false;
        ImGui.begin("Schematic Editor", ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse);

        ImVec2 canvasPos = ImGui.getCursorScreenPos();
        ImVec2 canvasSize = ImGui.getContentRegionAvail();
        ImDrawList drawList = ImGui.getWindowDrawList();

        if (ImGui.isWindowHovered() && ImGui.isMouseDragging(ImGuiMouseButton.Middle)) {
            scrolling.x += ImGui.getIO().getMouseDeltaX();
            scrolling.y += ImGui.getIO().getMouseDeltaY();
        }

        float wheel = ImGui.getIO().getMouseWheel();
        if (ImGui.isWindowHovered() && wheel != 0) {
            ImVec2 mousePos = ImGui.getMousePos();

            float mouseLocalX = (mousePos.x - canvasPos.x - scrolling.x) / zoom;
            float mouseLocalY = (mousePos.y - canvasPos.y - scrolling.y) / zoom;

            float oldZoom = zoom;
            zoom = Math.max(0.25f, Math.min(zoom + wheel * 0.1f * zoom, 3.0f));
            scrolling.x -= (mouseLocalX * zoom - mouseLocalX * oldZoom);
            scrolling.y -= (mouseLocalY * zoom - mouseLocalY * oldZoom);
        }

        float gridStep = 64.0f * zoom;
        int gridColor = ImGui.getColorU32(0.2f, 0.2f, 0.2f, 1.0f);
        for (float x = scrolling.x % gridStep; x < canvasSize.x; x += gridStep)
            drawList.addLine(canvasPos.x + x, canvasPos.y, canvasPos.x + x, canvasPos.y + canvasSize.y, gridColor);
        for (float y = scrolling.y % gridStep; y < canvasSize.y; y += gridStep)
            drawList.addLine(canvasPos.x, canvasPos.y + y, canvasPos.x + canvasSize.x, canvasPos.y + y, gridColor);

        for (int i = 0; i < links.size(); i++) {
            Pair<Integer, Integer> link = links.get(i);
            Pin start = findPin(link.getLeft());
            Pin end = findPin(link.getRight());

            // Auto-remove redundant reroutes
            if (start != null && end != null &&
                    start.lastScreenPos.x == end.lastScreenPos.x &&
                    start.lastScreenPos.y == end.lastScreenPos.y)
            {
                continue;
            }

            if (start != null && end != null) {
                boolean hovered = isMouseOverBezier(start.lastScreenPos, end.lastScreenPos, zoom);
                if (hovered && ImGui.isMouseClicked(ImGuiMouseButton.Left) && activePin == null && selectedNode == null && !rerouteCreatedThisFrame) {
                    ImVec2 m = ImGui.getMousePos();

                    float worldX = (m.x - canvasPos.x - scrolling.x) / zoom;
                    float worldY = (m.y - canvasPos.y - scrolling.y) / zoom;

                    Component reroute = Component.createReroute(worldX - 10, worldY - 10);
                    componentList.add(reroute);

                    int originalStartId = link.getLeft();
                    int originalEndId = link.getRight();

                    links.remove(i);

                    links.add(new Pair<>(originalStartId, reroute.pins.get(0).identifier));
                    links.add(new Pair<>(reroute.pins.get(1).identifier, originalEndId));

                    selectedNode = reroute;
                    break;
                }

                drawBezier(
                        drawList,
                        start.lastScreenPos,
                        end.lastScreenPos,
                        zoom,
                        hovered
                );

                // Check for dubbel click on a link
                if (ImGui.isMouseDoubleClicked(ImGuiMouseButton.Left)) {
                    if (isMouseOverBezier(start.lastScreenPos, end.lastScreenPos, zoom)) {
                        ImVec2 m = ImGui.getMousePos();
                        float worldX = (m.x - canvasPos.x - scrolling.x) / zoom;
                        float worldY = (m.y - canvasPos.y - scrolling.y) / zoom;

                        Component reroute = Component.createReroute(worldX - 10, worldY - 10);
                        componentList.add(reroute);

                        int originalStartId = link.getLeft();
                        int originalEndId = link.getRight();

                        links.remove(i);
                        links.add(new Pair<>(reroute.pins.get(1).identifier, originalEndId)); // Reroute Out -> End
                        links.add(new Pair<>(originalStartId, reroute.pins.get(0).identifier)); // Start -> Reroute In
                        break;
                    }
                }
            }
        }

        if (activePin != null) {
            ImVec2 mousePos = ImGui.getMousePos();
            drawBezier(drawList, activePin.lastScreenPos, mousePos, zoom, true);

            if (ImGui.isMouseReleased(ImGuiMouseButton.Left)) {
                Pin hoveredPin = findHoveredPin();
                if (hoveredPin != null && hoveredPin != activePin && hoveredPin.pinType != activePin.pinType) {
                    links.add(new Pair<>(activePin.identifier, hoveredPin.identifier));
                }
                activePin = null;
            }
        }

        for (Component component : componentList) {
            component.render(drawList, canvasPos, scrolling, zoom);
        }

        if (addComponentPopupOpen) {
            renderAddComponentPopup();
        }

        ImGui.end();
    }

    private static boolean isMouseOverBezier(ImVec2 p0, ImVec2 p3, float zoom) {
        ImVec2 mouse = ImGui.getMousePos();
        float cpOffset = 50.0f * zoom;
        ImVec2 p1 = new ImVec2(p0.x + cpOffset, p0.y);
        ImVec2 p2 = new ImVec2(p3.x - cpOffset, p3.y);

        int split = 50;
        for (int i = 0; i <= split; i++) {
            float t = (float) i / split;
            // Cubic Bezier formula
            float u = 1 - t;
            float x = u*u*u*p0.x + 3*u*u*t*p1.x + 3*u*t*t*p2.x + t*t*t*p3.x;
            float y = u*u*u*p0.y + 3*u*u*t*p1.y + 3*u*t*t*p2.y + t*t*t*p3.y;

            float dx = x - mouse.x;
            float dy = y - mouse.y;
            if ((dx*dx + dy*dy) < (100 * zoom)) return true; // 10px marge
        }
        return false;
    }


    private static Pin findPin(int id) {
        return pinLookup.get(id);
    }

    private static void drawBezier(
            ImDrawList drawList,
            ImVec2 start,
            ImVec2 end,
            float zoom,
            boolean hovered
    ) {
        int color = hovered
                ? ImGui.getColorU32(1f,1f,0.2f,1f)
                : ImGui.getColorU32(1f,0.8f,0f,1f);

        float thickness = hovered ? 4f * zoom : 2f * zoom;

        float dy = Math.abs(start.y - end.y);

        // If pins are nearly horizontal, draw straight line
        if (dy < 10 * zoom) {
            drawList.addLine(
                    start.x,
                    start.y,
                    end.x,
                    end.y,
                    color,
                    thickness
            );
            return;
        }

        float cpOffset = 60.0f * zoom;

        drawList.addBezierCubic(
                start.x, start.y,
                start.x + cpOffset, start.y,
                end.x - cpOffset, end.y,
                end.x, end.y,
                color,
                thickness
        );
    }

    private static Pin findHoveredPin() {
        ImVec2 mousePos = ImGui.getMousePos();
        for (Component c : componentList) {
            for (Pin p : c.pins) {
                float dx = mousePos.x - p.lastScreenPos.x;
                float dy = mousePos.y - p.lastScreenPos.y;
                if ((dx * dx + dy * dy) < 100) return p; // 10px radius
            }
        }
        return null;
    }

    private static int nextID() { return ++currentID; }

    public enum PinType { INPUT, OUTPUT }

    public static class Pin {
        int identifier = nextID();
        String pinName;
        PinType pinType;
        ImVec2 lastScreenPos = new ImVec2();

        public Pin(String pinName, PinType pinType) {
            this.pinName = pinName;
            this.pinType = pinType;
            pinLookup.put(identifier, this);
        }

        public void render(ImDrawList drawList, ImVec2 pos, float nodeWidth, float zoom) {
            float circleRadius = 6.5f * zoom;
            float px = (pinType == PinType.INPUT) ? pos.x : pos.x + nodeWidth;
            float py = pos.y;
            lastScreenPos.set(px, py);

            drawList.addCircleFilled(px, py, circleRadius, ImGui.getColorU32(ImGuiCol.PlotLines));

            ImGui.setCursorScreenPos(px - 10 * zoom, py - 10 * zoom);
            if (ImGui.invisibleButton("pin_btn" + identifier, 24 * zoom, 24 * zoom)) {
                // Clicked
            }

            if (ImGui.isItemActive() && ImGui.isMouseClicked(ImGuiMouseButton.Left)) {
                activePin = this;
            }

            if (zoom > 0.5f) {
                ImVec2 textSize = ImGui.calcTextSize(pinName);
                float textX = (pinType == PinType.INPUT)
                        ? px + (10 * zoom)
                        : px - (textSize.x * zoom) - (10 * zoom);

                float textY = py - (10 * zoom);

                drawList.addText(
                        textX,
                        textY,
                        ImGui.getColorU32(1f,1f,1f,1f),
                        pinName
                );
            }
        }
    }

    private static class Component {
        int identifier = nextID();
        String name;
        ArrayList<Pin> pins;
        ImVec2 pos; // Manual position
        ImVec2 size = new ImVec2(180, 70);
        boolean isReroute = false;

        public Component(String name, ArrayList<Pin> pins, float x, float y) {
            this.name = name;
            this.pins = pins;
            this.pos = new ImVec2(x, y);

            if (!pins.isEmpty()) {
                String longestName = "";
                for (Pin pin : pins) {
                    if (pin.pinName.length() > longestName.length()) {
                        longestName = pin.pinName;
                    }
                    size.y += 24;
                }
                float textWidth = ImGui.calcTextSize(longestName).x;
                size.x = Math.max(size.x, textWidth + 80);
            }
        }

        public static Component createReroute(float x, float y) {
            Component c = new Component("Reroute", new ArrayList<>(List.of(
                    new Pin("In", PinType.INPUT),
                    new Pin("Out", PinType.OUTPUT))), x, y);
            c.isReroute = true;
            c.size.set(20, 20);
            return c;
        }

        private static ImVec2 worldToScreen(ImVec2 world, ImVec2 origin) {
            return new ImVec2(
                    origin.x + scrolling.x + (world.x * zoom),
                    origin.y + scrolling.y + (world.y * zoom)
            );
        }

        private static ImVec2 screenToWorld(ImVec2 screen, ImVec2 origin) {
            return new ImVec2(
                    (screen.x - origin.x - scrolling.x) / zoom,
                    (screen.y - origin.y - scrolling.y) / zoom
            );
        }

        public void render(ImDrawList drawList, ImVec2 origin, ImVec2 scroll, float zoom) {
            float windowPosX = origin.x + scroll.x + (pos.x * zoom);
            float windowPosY = origin.y + scroll.y + (pos.y * zoom);
            float nodeSizeX = size.x * zoom;
            float nodeSizeY = size.y * zoom;

            ImGui.setCursorScreenPos(windowPosX, windowPosY);
            ImGui.invisibleButton("node_drag" + identifier, nodeSizeX, nodeSizeY);
            if (ImGui.isItemClicked(ImGuiMouseButton.Left)) {
                selectedNode = this;
            }
            if (ImGui.isItemActive() && ImGui.isMouseDragging(ImGuiMouseButton.Left, 2.0f)) {
                pos.x += ImGui.getIO().getMouseDeltaX() / zoom;
                pos.y += ImGui.getIO().getMouseDeltaY() / zoom;
            }
            if (ImGui.isItemDeactivatedAfterEdit()) {
                pos.x = Math.round(pos.x / GRID_SIZE) * GRID_SIZE;
                pos.y = Math.round(pos.y / GRID_SIZE) * GRID_SIZE;
            }

            if (isReroute) {
                float cx = windowPosX + nodeSizeX / 2;
                float cy = windowPosY + nodeSizeY / 2;

                ImGui.setCursorScreenPos(cx - 8 * zoom, cy - 8 * zoom);
                ImGui.invisibleButton("node_drag" + identifier, 16 * zoom, 16 * zoom);

                if (ImGui.isItemActive()) {
                    pos.x += ImGui.getIO().getMouseDeltaX() / zoom;
                    pos.y += ImGui.getIO().getMouseDeltaY() / zoom;
                }

                if (ImGui.isItemDeactivatedAfterEdit()) {
                    pos.x = Math.round(pos.x / GRID_SIZE) * GRID_SIZE;
                    pos.y = Math.round(pos.y / GRID_SIZE) * GRID_SIZE;
                }

                drawList.addCircleFilled(cx, cy, 5.0f * zoom, ImGui.getColorU32(ImGuiCol.WindowBg));
                drawList.addCircle(cx, cy, 6.0f * zoom, ImGui.getColorU32(ImGuiCol.PlotLines), 12, 2f * zoom);

                pins.get(0).lastScreenPos.set(cx, cy);
                pins.get(1).lastScreenPos.set(cx, cy);
            } else {
                drawList.addRectFilled(windowPosX + 4, windowPosY + 4, windowPosX + nodeSizeX + 4, windowPosY + nodeSizeY + 4, ImGui.getColorU32(0f,0f,0f,0.25f), 5.0f); // drop shadow
                drawList.addRectFilled(windowPosX, windowPosY, windowPosX + nodeSizeX, windowPosY + nodeSizeY, ImGui.getColorU32(ImGuiCol.WindowBg), 5.0f);
                int borderColor = (selectedNode == this)
                        ? ImGui.getColorU32(1f,0.7f,0.2f,1f)
                        : ImGui.getColorU32(ImGuiCol.Border);

                drawList.addRect(
                        windowPosX,
                        windowPosY,
                        windowPosX + nodeSizeX,
                        windowPosY + nodeSizeY,
                        borderColor,
                        5.0f,
                        0,
                        2.0f
                );

                drawList.addRectFilled(windowPosX, windowPosY, windowPosX + nodeSizeX, windowPosY + (26 * zoom), ImGui.getColorU32(ImGuiCol.Header), 5.0f);
                ImGui.setCursorScreenPos(windowPosX + 5 * zoom, windowPosY + 2 * zoom);

                ImGui.getFont().setScale(zoom);
                ImGui.text(name);
                ImGui.getFont().setScale(1.0f);

                float pinOffsetY = 45 * zoom;
                for (Pin pin : pins) {
                    pin.render(drawList, new ImVec2(windowPosX, windowPosY + pinOffsetY), nodeSizeX, zoom);
                    pinOffsetY += 20 * zoom;
                }
            }
        }
    }

    private static void renderAddComponentPopup() {
        ImVec2 windowPos = ImGui.getWindowPos();
        ImVec2 windowSize = ImGui.getWindowSize();

        float width = 520;
        float height = 420;

        ImGui.setNextWindowSize(width, height);
        ImGui.setNextWindowPos(
                windowPos.x + windowSize.x * 0.5f - width * 0.5f,
                windowPos.y + windowSize.y * 0.5f - height * 0.5f
        );

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 14f);
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 16, 16);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 6f);

        ImGui.begin(
                "Add Component",
                ImGuiWindowFlags.NoResize |
                        ImGuiWindowFlags.NoCollapse |
                        ImGuiWindowFlags.NoMove |
                        ImGuiWindowFlags.NoTitleBar
        );

        renderSearchBar();
        ImGui.separator();
        renderResults();

        ImGui.end();

        ImGui.popStyleVar(3);
    }

    private static void renderSearchBar() {

        ImGui.setNextItemWidth(-1);

        if (ImGui.inputTextWithHint(
                "##search",
                "Search for a component...",
                searchQueryBuffer
        )) {
            searchQuery = searchQueryBuffer.get();

            // If user typed something, don't select browser entry
            if (searchQuery.isEmpty()) {
                selectedIndex = 0;
            } else {
                selectedIndex = 1;
            }
        }

        ImGui.setKeyboardFocusHere(-1);
    }

    private static List<String> getFilteredResults() {

        if (searchQuery.isEmpty())
            return allComponents;

        String query = searchQuery.toLowerCase();

        List<Pair<String, Integer>> scored = new ArrayList<>();

        for (String comp : allComponents) {

            int score = fuzzyScore(query, comp.toLowerCase());

            if (score >= 0)
                scored.add(new Pair<>(comp, score));
        }

        scored.sort((a, b) -> Integer.compare(b.getRight(), a.getRight()));

        List<String> results = new ArrayList<>();

        for (int i = 0; i < scored.size() && i < MAX_RESULTS; i++) {
            results.add(scored.get(i).getLeft());
        }

        return results;
    }

    private static int fuzzyScore(String query, String target) {

        int score = 0;
        int queryIndex = 0;
        int consecutive = 0;

        for (int i = 0; i < target.length(); i++) {

            if (queryIndex >= query.length())
                break;

            char qc = query.charAt(queryIndex);
            char tc = target.charAt(i);

            if (qc == tc) {

                queryIndex++;
                consecutive++;

                score += 10 + consecutive * 5;

            } else {

                consecutive = 0;
            }
        }

        if (queryIndex != query.length())
            return -1;

        return score;
    }

    private static void renderResults() {
        List<String> results = getFilteredResults();
        int total = results.size() + 1;

        handleKeyboard(total);

        ImGui.beginChild("results");

        renderEntry("Open Component Browser...", 0);

        ImGui.separator();
        ImGui.spacing();

        if (results.isEmpty()) {
            ImGui.textDisabled("No components found");
        }

        for (int i = 0; i < results.size(); i++) {
            renderEntry(results.get(i), i + 1);
        }

        ImGui.endChild();
    }

    private static void renderEntry(String label, int index) {
        boolean selected = selectedIndex == index;

        if (selected)
            ImGui.pushStyleColor(ImGuiCol.Header, 0.25f,0.45f,0.9f,1);

        if (ImGui.selectable(label, selected)) {
            selectedIndex = index;
            activateSelected();
        }

        if (selected)
            ImGui.popStyleColor();
    }

    private static boolean upHeld = false;
    private static boolean downHeld = false;
    private static boolean enterHeld = false;
    private static boolean escapeHeld = false;

    private static void handleKeyboard(int totalResults) {
        Window window = MinecraftClient.getInstance().getWindow();

        boolean up = InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_UP);
        boolean down = InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_DOWN);
        boolean enter = InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_ENTER);
        boolean escape = InputUtil.isKeyPressed(window, InputUtil.GLFW_KEY_ESCAPE);

        if (down && !downHeld) {
            selectedIndex++;
            if (selectedIndex >= totalResults)
                selectedIndex = totalResults - 1;
        }

        if (up && !upHeld) {
            selectedIndex--;
            if (selectedIndex < 0)
                selectedIndex = 0;
        }

        if (enter && !enterHeld) {
            activateSelected();
        }

        if (escape && !escapeHeld) {
            addComponentPopupOpen = false;
        }

        downHeld = down;
        upHeld = up;
        enterHeld = enter;
        escapeHeld = escape;
    }

    private static void activateSelected() {
        List<String> results = getFilteredResults();

        if (selectedIndex == 0) {
            //openComponentBrowserWindow();
            addComponentPopupOpen = false;
            return;
        }

        int componentIndex = selectedIndex - 1;

        if (componentIndex < results.size()) {

            String component = results.get(componentIndex);

            createComponent(component);

            addComponentPopupOpen = false;
        }
    }

    private static void createComponent(String name) {
        System.out.println("Create component: " + name);
    }

    public static void openPopup() {
        SchematicEditorWindow.addComponentPopupOpen = true;
        upHeld = false;
        downHeld = false;
        enterHeld = false;
        escapeHeld = false;
    }
}
