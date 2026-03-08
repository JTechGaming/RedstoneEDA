package com.cybrisoft.redstoneeda.client.helpers;

public class EditorHelper {
    public enum EditorType {
        NONE("Main"),
        SCHEMATIC("Preview"),
        FOOTPRINT("Footprint");

        final String title;
        EditorType(String title) {
            this.title = title;
        }
        public String getTitle() {
            return title;
        }
    }

    private static EditorType currentEditorType = EditorType.NONE;

    public static EditorType getCurrentEditorType() {
        return currentEditorType;
    }

    public static void setCurrentEditorType(EditorType currentEditorType) {
        EditorHelper.currentEditorType = currentEditorType;
    }
}
