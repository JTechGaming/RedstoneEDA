package com.cybrisoft.redstoneeda.client.uiElements.windows;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.breakpoints.BinaryCondition;
import com.cybrisoft.redstoneeda.breakpoints.BlockstateCondition;
import com.cybrisoft.redstoneeda.breakpoints.Breakpoint;
import com.cybrisoft.redstoneeda.client.RedstoneedaClient;
import com.cybrisoft.redstoneeda.client.breakpoints.ClientBreakpoint;
import com.cybrisoft.redstoneeda.breakpoints.ConditionTypes;
import com.cybrisoft.redstoneeda.client.imgui.ImGuiImplementation;
import com.cybrisoft.redstoneeda.networking.C2S.C2SBreakpointPacket;
import imgui.ImColor;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;

import java.util.*;

@Environment(EnvType.CLIENT)
public class BreakpointWindow {
    public static ImBoolean isOpen = new ImBoolean(true);

    private static ImBoolean creatingBreakpoint = new ImBoolean(false);
    private static ClientBreakpoint breakpoint = null;
    private static ImString filter = new ImString();

    private static Map<String, ImInt> intWidgetCache = new HashMap<>();
    private static Map<String, ImInt> enumWidgetCache = new HashMap<>();
    private static Map<String, ImBoolean> boolWidgetCache = new HashMap<>();

    private static List<ClientBreakpoint> breakpoints = new ArrayList<>();

    private static int prevIndex = 0;
    private static boolean lastFrameNoProject = false;
    private static boolean needProjectRefresh = false;

    private static int syncBpToServer = -1;

    public static void render() {
        if (!isOpen.get()) {
            return; // If the window is not open, do not render
        }

        if (ImGui.begin("Breakpoints", isOpen, ImGuiWindowFlags.MenuBar)) {
            if (RedstoneedaClient.getClientProject() == null) {
                lastFrameNoProject = true;
                breakpoints.clear();

                ImGuiImplementation.centeredText("No Project Loaded");

                ImGui.end();
                return;
            }

            if (lastFrameNoProject || needProjectRefresh) {
                lastFrameNoProject = false;
                needProjectRefresh = false;
                loadBreakpoints(RedstoneedaClient.getClientProject());
            }

            if (ImGui.beginMenuBar()) {
                if (ImGui.menuItem("New Breakpoint", null, creatingBreakpoint)) {
                    breakpoint = new ClientBreakpoint(UUID.randomUUID());
                }
                ImGui.endMenuBar();
            }

            if (breakpoints.isEmpty()) {
                ImGuiImplementation.centeredText("No breakpoints found");
            } else {
                for (int i = 0; i < breakpoints.size(); i++) {
                    ClientBreakpoint bp = breakpoints.get(i);

                    ImVec2 pos = ImGui.getCursorScreenPos();
                    float radius = 5f;
                    int color = ImColor.rgba(220, 30, 30, bp.isActive() ? 255 : 100);
                    ImGui.getWindowDrawList().addCircleFilled(pos.x + radius, pos.y + radius / 2, radius, color);
                    ImGui.dummy(radius * 2, radius * 2);
                    if (ImGui.isItemClicked()) {
                        bp.setActive(!bp.isActive());
                        syncBpToServer = i;
                    }
                    ImGui.sameLine();
                    ImGui.text("Name: ");
                    ImGui.sameLine();
                    if (ImGui.selectable(bp.getName().get() + "##breakpointwindowforloop" + i)) {
                        breakpoint = bp;
                    }
                    ImGui.sameLine();
                    ImGui.text("Condition: ");
                    ImGui.sameLine();
                    ImGui.text(bp.getCondition().getName());
                }
            }
        }
        ImGui.end();

        if (breakpoint != null) {
            drawCreateBreakpoint();
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, Object value) {
        return state.with(property, (T) value);
    }

    private static void drawCreateBreakpoint() {
        ImVec2 centerPos = ImGuiImplementation.getCenterViewportPos();
        ImGui.setNextWindowPos(centerPos.x, centerPos.y, ImGuiCond.Always, 0.5f, 0.5f);

        if (ImGui.begin("New Breakpoint", creatingBreakpoint)) {
            ImGui.text("Active: ");
            ImGui.sameLine();
            ImGui.checkbox("##breakpointactive", breakpoint.getActive());

            ImGui.text("Name: ");
            ImGui.sameLine();
            ImGui.inputText("##breakpointname", breakpoint.getName());

            ImGui.text("Should pause game: ");
            ImGui.sameLine();
            ImGui.checkbox("##shouldpausegame", breakpoint.getShouldPauseGame());

            ImGui.text("Disable on trigger: ");
            ImGui.sameLine();
            ImGui.checkbox("##disableontrigger", breakpoint.getDisableOnTrigger());

            ImGui.text("Condition: ");
            ImGui.sameLine();
            if (ImGui.beginListBox("##conditionlist")) {
                for (ConditionTypes type : ConditionTypes.values()) {
                    if (ImGui.menuItem(type.name())) {
                        switch (type) {
                            case NONE -> breakpoint.setCondition(null);
                            case BINARY -> breakpoint.setCondition(new BinaryCondition(breakpoint.getUuid()));
                            case STATE -> breakpoint.setCondition(new BlockstateCondition(breakpoint.getUuid()));
                        }
                    }
                }
                ImGui.endListBox();
            }

            if (ImGui.beginChild("##conditioneditor")) {
                switch (breakpoint.getCondition()) {
                    case null -> {
                        ImGui.text("No condition selected");
                        ImGui.endChild();
                        ImGui.end();
                        return;
                    }
                    case BlockstateCondition condition -> {
                        if (ImGui.button("New block")) {
                            condition.expectedPositions.add(new int[]{0, 0, 0});
                            condition.expectedStates.add(Blocks.AIR.getDefaultState());
                        }
                        for (int i = 0; i < condition.expectedPositions.size(); i++) {
                            ImGui.pushStyleColor(ImGuiCol.ChildBg, 0.15f, 0.15f, 0.15f, 1f);
                            if (ImGui.beginChild("##blockstateposconf" + i, 0, 120, true)) {
                                if (i != prevIndex) {
                                    boolWidgetCache.clear();
                                    enumWidgetCache.clear();
                                    intWidgetCache.clear();
                                }
                                ImGui.text("Position: ");
                                ImGui.sameLine();
                                ImGui.inputInt3("##blockposselect" + i, condition.expectedPositions.get(i));
                                ImGui.sameLine();
                                ImGui.text("Block: ");
                                ImGui.sameLine();
                                String name = condition.expectedStates.get(i) != null ? condition.expectedStates.get(i).getBlock().getName().getString() : "None";
                                ImGui.selectable(name + "##blocktypeselect" + i);
                                if (ImGui.beginPopupContextItem(name)) {
                                    ImGui.inputText("Filter: ", filter);

                                    int finalI = i;
                                    Registries.BLOCK.forEach(block -> {
                                        if (filter.isNotEmpty() && !block.getName().getString().toLowerCase().contains(filter.get().toLowerCase()))
                                            return;
                                        if (ImGui.menuItem(block.getName().getString() + "##blocktypelist")) {
                                            condition.expectedStates.set(finalI, block.getDefaultState());
                                        }
                                    });
                                    ImGui.endPopup();
                                }

                                BlockState state = condition.expectedStates.get(i);

                                BlockState finalState = state;

                                for (Property<?> property : state.getProperties()) {
                                    String propertyName = "##property" + i + "__" + property.getName();
                                    ImGui.text(property.getName() + "##property" + i);
                                    ImGui.sameLine();

                                    if (property instanceof BooleanProperty) {
                                        ImBoolean widget = boolWidgetCache.computeIfAbsent(propertyName, k -> new ImBoolean((Boolean) finalState.get(property)));
                                        if (ImGui.checkbox(propertyName, widget)) {
                                            state = applyProperty(state, property, widget.get());
                                            condition.expectedStates.set(i, state);
                                        }
                                    } else if (property instanceof EnumProperty<?>) {
                                        List<?> values = new ArrayList<>(property.getValues());
                                        String[] names = values.stream().map(Object::toString).toArray(String[]::new);
                                        int currentIndex = values.indexOf(state.get(property));
                                        ImInt widget = enumWidgetCache.computeIfAbsent(propertyName, k -> new ImInt(currentIndex));

                                        if (ImGui.combo(propertyName, widget, names)) {
                                            state = applyProperty(state, property, values.get(widget.get()));
                                            condition.expectedStates.set(i, state);
                                        }
                                    } else if (property instanceof IntProperty) {
                                        ImInt widget = intWidgetCache.computeIfAbsent(propertyName, k -> new ImInt((Integer) finalState.get(property)));
                                        if (ImGui.inputInt(propertyName, widget)) {
                                            Collection<Integer> validValues = ((IntProperty) property).getValues();
                                            int clamped = Math.max(Collections.min(validValues), Math.min(Collections.max(validValues), widget.get()));
                                            widget.set(clamped);
                                            state = applyProperty(state, property, clamped);
                                            condition.expectedStates.set(i, state);
                                        }
                                    }
                                }
                                prevIndex = i;
                            }
                            ImGui.endChild();
                            ImGui.popStyleColor();
                        }
                    }
                    case BinaryCondition condition -> {

                    }
                    default -> {
                    }
                }
            }
            ImGui.endChild();

            ClientBreakpoint bp = findBreakpoint(breakpoint.getUuid());
            if (ImGui.button(bp == null ? "Create" : "Update")) {
                if (bp == null) {
                    breakpoints.add(breakpoint);
                    syncBpToServer = breakpoints.indexOf(breakpoint);
                } else {
                    breakpoints.set(breakpoints.indexOf(bp), breakpoint);
                    syncBpToServer = breakpoints.indexOf(bp);
                }

                breakpoint = null;
            }
        }
        ImGui.end();
    }

    public static void invalidateProject() {
        needProjectRefresh = true;
    }

    /**
     * Uploading is done in the tick method instead of the render method mainly because the
     * render method can be called hundreds of times per second, while the tick method only gets
     * called 20 times a second. So this works to throttle the rate of packets sent to the server.
    **/
    public static void tick() {
        if (syncBpToServer < 0 || syncBpToServer >= breakpoints.size()) return;

        ClientBreakpoint bp = breakpoints.get(syncBpToServer);

        uploadBreakpoint(bp);

        syncBpToServer = -1;
    }

    private static ClientBreakpoint findBreakpoint(UUID uuid) {
        for (ClientBreakpoint elem : breakpoints) {
            if (elem.getUuid() == breakpoint.getUuid()) {
                return elem;
            }
        }
        return null;
    }

    public static void loadBreakpoints(Project project) {
        breakpoints.clear();
        for (Breakpoint bp : project.getBreakpoints()) {
            breakpoints.add(new ClientBreakpoint(bp.getUuid(), bp.getName(), bp.isShouldPauseGame(), bp.isDisableOnTrigger(), bp.getCondition()));
        }
    }

    private static void uploadBreakpoint(ClientBreakpoint bp) {
        ClientPlayNetworking.send(new C2SBreakpointPacket(new Breakpoint(bp.getUuid(), bp.getName().get(), bp.getShouldPauseGame().get(), bp.getDisableOnTrigger().get(), bp.getCondition())));
    }
}
