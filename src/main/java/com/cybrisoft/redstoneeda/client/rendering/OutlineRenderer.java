package com.cybrisoft.redstoneeda.client.rendering;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

public class OutlineRenderer {
    // Renders an outline around a given selection of blocks in the Minecraft world.

    public static Map<BlockPos, BlockPos> outlines = new HashMap<>();

    public static void init() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register((context) -> {
            Vec3d cameraPos = context.gameRenderer().getCamera().getCameraPos();
            for (BlockPos pos1 : new HashMap<>(outlines).keySet()) {
                if (pos1 == null || outlines.get(pos1) == null) continue;
                BlockPos pos2 = outlines.get(pos1);
                Box box = new Box(
                        Math.min(pos1.getX(), pos2.getX()) - cameraPos.x,
                        Math.min(pos1.getY(), pos2.getY()) - cameraPos.y,
                        Math.min(pos1.getZ(), pos2.getZ()) - cameraPos.z,
                        Math.max(pos1.getX(), pos2.getX()) + 1 - cameraPos.x,
                        Math.max(pos1.getY(), pos2.getY()) + 1 - cameraPos.y,
                        Math.max(pos1.getZ(), pos2.getZ()) + 1 - cameraPos.z
                );
                renderOutline(context, box.expand(0.002)); // Slightly expand the box to prevent z-fighting
            }
        });
    }

    public static void renderOutline(WorldRenderContext context, Box box) {
        Matrix4f transformationMatrix = context.matrices().peek().getPositionMatrix();

        GlStateManager._enableBlend();

        VertexConsumer buffer =  context.consumers().getBuffer(RenderLayers.LINES);

        float red = 1.0f;
        float green = 0.0f;
        float blue = 0.0f;
        float alpha = 1.0f;

        // Top Face Edges
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 1, 0).lineWidth(10f);

        // Vertical Edges
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 0, -1).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 0, -1).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(1, 0, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(1, 0, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(1, 0, 1).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(1, 0, 1).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 0, 1).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 0, 1).lineWidth(10f);

        // Bottom Face Edges
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);

        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(0, -1, 0).lineWidth(10f);

        renderFaces(context, box);

        GlStateManager._disableBlend();
        GlStateManager._enableDepthTest();
    }

    public static void renderFaces(WorldRenderContext context, Box box) {
        Matrix4f transformationMatrix = context.matrices().peek().getPositionMatrix();

        GlStateManager._enableBlend();

        VertexConsumer buffer = context.consumers().getBuffer(RenderLayers.debugQuads());

        float red = 1.0f;
        float green = 0.0f;
        float blue = 0.0f;
        float alpha = 0.2f;

        // Top Face (Y = maxY)
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 1, 0);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 1, 0);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 1, 0);

        // Bottom Face (Y = minY)
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(0, -1, 0);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(0, -1, 0);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, -1, 0);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, -1, 0);

        // Front Face (Z = minZ)
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 0, -1);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 0, -1);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 0, -1);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(0, 0, -1);

        // Back Face (Z = maxZ)
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 0, 1);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 0, 1);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 0, 1);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(0, 0, 1);

        // Left Face (X = minX)
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(-1, 0, 0);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(-1, 0, 0);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(-1, 0, 0);
        buffer.vertex(transformationMatrix, (float) box.minX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(-1, 0, 0);

        // Right Face (X = maxX)
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.minZ).color(red, green, blue, alpha).normal(1, 0, 0);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.minZ).color(red, green, blue, alpha).normal(1, 0, 0);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.maxY, (float) box.maxZ).color(red, green, blue, alpha).normal(1, 0, 0);
        buffer.vertex(transformationMatrix, (float) box.maxX, (float) box.minY, (float) box.maxZ).color(red, green, blue, alpha).normal(1, 0, 0);

        GlStateManager._disableBlend();
        GlStateManager._enableDepthTest();
    }
}
