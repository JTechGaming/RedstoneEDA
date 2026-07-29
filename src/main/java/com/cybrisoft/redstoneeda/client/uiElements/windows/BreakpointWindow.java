package com.cybrisoft.redstoneeda.client.uiElements.windows;

import com.cybrisoft.redstoneeda.client.breakpoints.ClientBinaryCondition;
import com.cybrisoft.redstoneeda.client.breakpoints.ClientBlockstateCondition;
import com.cybrisoft.redstoneeda.client.breakpoints.ClientBreakpoint;
import com.cybrisoft.redstoneeda.client.breakpoints.ConditionTypes;
import com.cybrisoft.redstoneeda.client.imgui.ImGuiImplementation;
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
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;

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

    public static void render() {
        if (!isOpen.get()) {
            return; // If the window is not open, do not render
        }
        if (ImGui.begin("Breakpoints", isOpen, ImGuiWindowFlags.MenuBar)) {
            if (ImGui.beginMenuBar()) {
                if (ImGui.menuItem("New Breakpoint", null, creatingBreakpoint)) {
                    breakpoint = new ClientBreakpoint(breakpoints.size());
                }
                ImGui.endMenuBar();
            }

            for (int i=0; i<breakpoints.size(); i++) {
                ClientBreakpoint bp = breakpoints.get(i);

                ImVec2 pos = ImGui.getCursorScreenPos();
                float radius = 5f;
                int color = ImColor.rgba(220, 30, 30, 100 + (bp.getActive() ? 155 : 0));
                ImGui.getWindowDrawList().addCircleFilled(pos.x + radius, pos.y + radius / 2, radius, color);
                ImGui.dummy(radius * 2, radius * 2);
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
                            case BINARY -> breakpoint.setCondition(new ClientBinaryCondition());
                            case STATE -> breakpoint.setCondition(new ClientBlockstateCondition());
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
                    case ClientBlockstateCondition condition -> {
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
                                        if (filter.isNotEmpty() && !block.getName().getString().contains(filter.get()))
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
                    case ClientBinaryCondition condition -> {

                    }
                    default -> {
                    }
                }
            }
            ImGui.endChild();

            if (ImGui.button(breakpoints.size() <= breakpoint.getId() ? "Create" : "Update")) {
                if (breakpoints.size() <= breakpoint.getId()) {
                    breakpoints.add(breakpoint);
                } else {
                    breakpoints.set(breakpoint.getId(), breakpoint);
                }

                breakpoint = null;
            }
        }
        ImGui.end();
    }
}
