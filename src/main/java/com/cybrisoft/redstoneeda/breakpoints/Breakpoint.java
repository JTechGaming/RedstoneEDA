package com.cybrisoft.redstoneeda.breakpoints;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

import java.util.UUID;

public class Breakpoint {
    private UUID uuid;
    private String name;
    private boolean shouldPauseGame;
    private boolean disableOnTrigger;
    private BreakpointCondition condition;
    private boolean isActive = true;

    public Breakpoint(UUID uuid, String name, boolean shouldPauseGame, boolean disableOnTrigger, BreakpointCondition condition) {
        this.uuid = uuid;
        this.name = name;
        this.shouldPauseGame = shouldPauseGame;
        this.disableOnTrigger = disableOnTrigger;
        this.condition = condition;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isShouldPauseGame() {
        return shouldPauseGame;
    }

    public void setShouldPauseGame(boolean shouldPauseGame) {
        this.shouldPauseGame = shouldPauseGame;
    }

    public boolean isDisableOnTrigger() {
        return disableOnTrigger;
    }

    public void setDisableOnTrigger(boolean disableOnTrigger) {
        this.disableOnTrigger = disableOnTrigger;
    }

    public BreakpointCondition getCondition() {
        return condition;
    }

    public void setCondition(BreakpointCondition condition) {
        this.condition = condition;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public static final PacketCodec<PacketByteBuf, Breakpoint> PACKET_CODEC = new PacketCodec<PacketByteBuf, Breakpoint>() {
        public Breakpoint decode(PacketByteBuf byteBuf) {
            return new Breakpoint(byteBuf.readUuid(), byteBuf.readString(), byteBuf.readBoolean(), byteBuf.readBoolean(), BreakpointCondition.PACKET_CODEC.decode(byteBuf));
        }

        public void encode(PacketByteBuf byteBuf, Breakpoint bp) {
            byteBuf.writeUuid(bp.getUuid());
            byteBuf.writeString(bp.getName());
            byteBuf.writeBoolean(bp.isShouldPauseGame());
            byteBuf.writeBoolean(bp.isDisableOnTrigger());
            BreakpointCondition.PACKET_CODEC.encode(byteBuf, bp.getCondition());
        }
    };
}
