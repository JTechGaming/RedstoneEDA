package com.cybrisoft.redstoneeda.client.uiElements.windows;

import imgui.ImGui;
import imgui.ImVec2;
import imgui.extension.imnodes.ImNodes;
import imgui.type.ImInt;
import net.minecraft.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class SchematicEditorWindow {
    static int currentID = 0;

    private static final List<Component> componentList = new ArrayList<>();
    private static final List<Pair<Integer, Integer>> links = new ArrayList<>();

    static {
        componentList.add(new Component(
                "Test Node",
                new ArrayList<>(List.of(
                        new Pin(
                                "Test Pin", PinType.OUTPUT
                        ),
                        new Pin(
                                "Test In Pin", PinType.INPUT
                        )
                )),
                80.0f, 80.0f
        ));
        componentList.add(new Component(
                "Test Node",
                new ArrayList<>(List.of(
                        new Pin(
                                "Test Pin", PinType.OUTPUT
                        ),
                        new Pin(
                                "Test In Pin", PinType.INPUT
                        )
                )),
                80.0f, 80.0f
        ));
    }

    public static void render() {
        ImGui.begin("Node Editor Test");

        ImNodes.beginNodeEditor();

        for (Component component : componentList) {
            component.render();
        }

        for (int i = 0; i < links.size(); ++i) {
            Pair<Integer, Integer> link = links.get(i);
            ImNodes.link(i, link.getLeft(), link.getRight());
        }

        ImNodes.endNodeEditor();

        ImGui.end();

        ImInt start = new ImInt(), end = new ImInt();
        if (ImNodes.isLinkCreated(start, end)) {
            links.add(new Pair<>(start.get(), end.get()));
        }
    }

    private static int nextID() {
        return ++currentID;
    }

    public enum PinType {
        INPUT,
        OUTPUT
    } // maybe add 'clock' and 'passive' types?

    public static class Pin {
        int identifier = nextID();
        String pinName;
        PinType pinType;

        public Pin(String pinName, PinType pinType) {
            this.pinName = pinName;
            this.pinType = pinType;
        }

        public void render() {
            switch (pinType) {
                case INPUT -> ImNodes.beginInputAttribute(identifier);
                case OUTPUT -> ImNodes.beginOutputAttribute(identifier);
            }

            ImGui.text(pinName);

            switch (pinType) {
                case INPUT -> ImNodes.endInputAttribute();
                case OUTPUT -> ImNodes.endOutputAttribute();
            }
        }

        public int getIdentifier() {
            return identifier;
        }
    }

    private static class Component {
        int identifier = nextID();
        String name;
        ArrayList<Pin> pins;
        ImVec2 size = new ImVec2(80.0f, 45.0f);

        public Component(String name, ArrayList<Pin> pins) {
            this.name = name;
            this.pins = pins;
        }

        public Component(String name, ArrayList<Pin> pins, ImVec2 size) {
            this.name = name;
            this.pins = pins;
            this.size = size;
        }

        public Component(String name, ArrayList<Pin> pins, float sizeX, float sizeY) {
            this.name = name;
            this.pins = pins;
            this.size.x = sizeX;
            this.size.y = sizeY;
        }

        public void render() {
            ImNodes.beginNode(identifier);
            for (Pin pin : pins) {
                pin.render();
            }
            ImGui.dummy(size.x, size.y);
            ImNodes.endNode();
        }

        public int getIdentifier() {
            return identifier;
        }
    }
}
