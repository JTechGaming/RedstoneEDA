package com.cybrisoft.redstoneeda.client.breakpoints;

import imgui.type.ImBoolean;
import imgui.type.ImString;

public class ClientBreakpoint {
    private int id = 0;
    private ImString name = new ImString();
    private ImBoolean shouldPauseGame = new ImBoolean(true);
    private ImBoolean disableOnTrigger = new ImBoolean(true);
    private ClientCondition condition;

    public ClientBreakpoint(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public ClientCondition getCondition() {
        return condition;
    }

    public void setCondition(ClientCondition condition) {
        this.condition = condition;
    }

    public boolean getActive() {
        return true;
    }
}

