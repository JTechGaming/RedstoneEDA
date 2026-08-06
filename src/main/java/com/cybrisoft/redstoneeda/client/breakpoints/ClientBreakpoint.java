package com.cybrisoft.redstoneeda.client.breakpoints;

import com.cybrisoft.redstoneeda.breakpoints.BreakpointCondition;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.util.UUID;

public class ClientBreakpoint {
    private UUID uuid;
    private ImString name = new ImString();
    private ImBoolean shouldPauseGame = new ImBoolean(true);
    private ImBoolean disableOnTrigger = new ImBoolean(true);
    private BreakpointCondition condition;
    private ImBoolean isActive = new ImBoolean(true);

    public ClientBreakpoint(UUID uuid) {
        this.uuid = uuid;
    }

    public ClientBreakpoint(UUID uuid, String name, boolean shouldPauseGame, boolean disableOnTrigger, BreakpointCondition condition) {
        this.uuid = uuid;
        this.name.set(name);
        this.shouldPauseGame.set(shouldPauseGame);
        this.disableOnTrigger.set(disableOnTrigger);
        this.condition = condition;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public ImString getName() {
        return name;
    }

    public void setName(ImString name) {
        this.name = name;
    }

    public ImBoolean getShouldPauseGame() {
        return shouldPauseGame;
    }

    public void setShouldPauseGame(ImBoolean shouldPauseGame) {
        this.shouldPauseGame = shouldPauseGame;
    }

    public ImBoolean getDisableOnTrigger() {
        return disableOnTrigger;
    }

    public void setDisableOnTrigger(ImBoolean disableOnTrigger) {
        this.disableOnTrigger = disableOnTrigger;
    }

    public BreakpointCondition getCondition() {
        return condition;
    }

    public void setCondition(BreakpointCondition condition) {
        this.condition = condition;
    }

    public boolean isActive() {
        return isActive.get();
    }

    public ImBoolean getActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive.set(active);
    }
}

