package com.cybrisoft.redstoneeda.client.rendering;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class WorldFeatureRenderer {
    private static List<BlockPos> warnings = new ArrayList<>();

    public static void addWarning(BlockPos pos) {
        warnings.add(pos);
    }

    public static void removeWarning(BlockPos pos) {
        warnings.remove(pos);
    }

    public static void clearWarnings() {
        warnings.clear();
    }

    public static void init() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register((context) -> {
            Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();

            for (BlockPos pos : warnings) {
                drawWarning(context, pos.toCenterPos().subtract(cameraPos));
            }
        });
    }

    private static void drawWarning(WorldRenderContext context, Vec3d pos) {
        Matrix4f transformationMatrix = context.matrices().peek().getPositionMatrix();

        GlStateManager._enableBlend();

        VertexConsumer buffer =  context.consumers().getBuffer(RenderLayers.LINES);

        float red = 0.9f;
        float green = 0.9f;
        float blue = 0.0f;
        float alpha = 1.0f;

        // Top Face Edges
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y+0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y+0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y+0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y+0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y+0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y+0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y+0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y+0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);

        // Vertical Edges
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y-0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, 0, -1).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y+0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, 0, -1).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y-0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(1, 0, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y+0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(1, 0, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y-0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(1, 0, 1).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y+0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(1, 0, 1).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y-0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, 0, 1).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y+0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, 0, 1).lineWidth(10f);

        // Bottom Face Edges
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y-0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y-0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y-0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y-0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y-0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y-0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y-0.5f, (float) pos.z+0.5f).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y-0.5f, (float) pos.z-0.5f).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);

        drawExclamationMark(context, transformationMatrix, pos.add(0, 1, 0));

        GlStateManager._disableBlend();
        GlStateManager._enableDepthTest();
    }

    private static void drawExclamationMark(WorldRenderContext context, Matrix4f transformationMatrix, Vec3d pos) {
        Vec3d camPos = context.gameRenderer().getCamera().getCameraPos();
        double dx = camPos.x - pos.x;
        double dz = camPos.z - pos.z;
        float yaw = (float) Math.atan2(dx, dz);

        float red = 0.9f;
        float green = 0.9f;
        float blue = 0.0f;
        float alpha = 1.0f;

        VertexConsumer buffer = context.consumers().getBuffer(RenderLayers.debugQuads());

        buffer.vertex(transformationMatrix, (float) pos.x, (float) pos.y-0.5f, (float) pos.z).color(red, green, blue, alpha).normal(0, 0, 1);
        buffer.vertex(transformationMatrix, (float) pos.x-0.5f, (float) pos.y+0.5f, (float) pos.z).color(red, green, blue, alpha).normal(0, 0, 1);
        buffer.vertex(transformationMatrix, (float) pos.x+0.5f, (float) pos.y+0.5f, (float) pos.z).color(red, green, blue, alpha).normal(0, 0, 1);
    }
}
