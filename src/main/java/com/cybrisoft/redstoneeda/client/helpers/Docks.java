package com.cybrisoft.redstoneeda.client.helpers;

public enum Docks {
    DEFAULT("default", () -> {
        DockingHelper.setStackTraceShouldSwap(true);
        DockingHelper.setVariableTrackerShouldSwap(true);
    }),
    DEBUGGER("debugger", () -> {
        DockingHelper.setStackTraceShouldSwap(true);
        DockingHelper.setVariableTrackerShouldSwap(true);
    })
    ;

    final String filename;
    final StateAction windowStates;

    Docks(String filename, StateAction windowStates) {
        this.filename = filename;
        this.windowStates = windowStates;
    }

    public String get() {
        return filename;
    }

    public void setup() {
        windowStates.call();
    }

    @FunctionalInterface
    public interface StateAction {
        void call();
    }
}
