package com.cybrisoft.redstoneeda.breakpoints;

public enum ConditionTypes {
    NONE("Select a condition"),
    BINARY("Binary"),
    STATE("Block State")
    ;

    final String name;

    ConditionTypes(String name) {
        this.name = name;
    }

    public static String[] names() {
        int count = ConditionTypes.values().length;
        String[] result = new String[count];
        for (int i=0; i<count; i++) {
            result[i] = ConditionTypes.values()[i].name();
        }
        return result;
    }

    public static ConditionTypes fromName(String name) {
        for (ConditionTypes type : ConditionTypes.values()) {
            if (type.name.equals(name)) {
                return type;
            }
        }
        return null;
    }
}
