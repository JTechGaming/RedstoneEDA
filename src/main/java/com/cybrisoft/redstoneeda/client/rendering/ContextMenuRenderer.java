package com.cybrisoft.redstoneeda.client.rendering;

import com.cybrisoft.redstoneeda.Redstoneeda;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

public class ContextMenuRenderer {
    private static boolean drawContextMenu = false;

    public static boolean shouldDrawContextMenu() {
        return drawContextMenu;
    }

    public static void setDrawContextMenu(boolean drawContextMenu) {
        ContextMenuRenderer.drawContextMenu = drawContextMenu;
    }

    public static void init() {
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of(Redstoneeda.MOD_ID, "context_menu"), ContextMenuRenderer::draw);
    }

    private static void draw(DrawContext graphics, RenderTickCounter tickCounter) {
        if (!shouldDrawContextMenu()) return;

        int width = graphics.getScaledWindowWidth();
        int height = graphics.getScaledWindowHeight();

        Identifier texture = Identifier.of(Redstoneeda.MOD_ID, "textures/ui/radial.png");

        int radialScale = 256;

        graphics.drawTexture(RenderPipelines.GUI_TEXTURED, texture, width/2 - radialScale/2, height/2 - radialScale/2, 0, 0, radialScale, radialScale, 256, 256);
    }
}
