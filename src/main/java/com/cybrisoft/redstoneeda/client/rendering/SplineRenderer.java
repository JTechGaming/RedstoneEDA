package com.cybrisoft.redstoneeda.client.rendering;

import com.cybrisoft.redstoneeda.Redstoneeda;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.MappableRingBuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CopyOnWriteArrayList;

public class SplineRenderer {
    public static final RenderPipeline LINES_THROUGH_WALLS = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Redstoneeda.identifier("pipeline/lines_through_walls"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );

    public static class Spline {
        Vec3d[] points;
        Color color;

        public Spline(Vec3d[] points, Color color) {
            this.points = points;
            this.color = color;
        }

        public Vec3d[] getPoints() {
            return points;
        }

        public void addPoint(Vec3d point) {
            points = Arrays.copyOf(points, points.length + 1);
            points[points.length - 1] = point;
        }
    }

    public static List<Spline> splines = new CopyOnWriteArrayList<>();
    private static final BufferAllocator allocator = new BufferAllocator(RenderLayer.field_64009);
    private static BufferBuilder buffer;

    public static void init() {
        WorldRenderEvents.BEFORE_TRANSLUCENT.register((context) -> {
            MatrixStack matrices = context.matrices();
            Vec3d camera = context.worldState().cameraRenderState.pos;

            matrices.push();
            matrices.translate(-camera.x, -camera.y, -camera.z);

            if (buffer == null) {
                buffer = new BufferBuilder(allocator, LINES_THROUGH_WALLS.getVertexFormatMode(), LINES_THROUGH_WALLS.getVertexFormat());
            }

            Matrix4fc positionMatrix = matrices.peek().getPositionMatrix();
            for (Spline spline : splines) {
                for (int i=0; i<spline.points.length;i++) {
                    Vec3d point = spline.points[i];
                    if (i > 0) {
                        drawVertex(buffer, positionMatrix, spline.points[i-1], spline.color);
                        drawVertex(buffer, positionMatrix, point, spline.color);
                    }
                }
            }

            matrices.pop();

            drawLinesThroughWalls(MinecraftClient.getInstance(), LINES_THROUGH_WALLS);
        });

        splines.add(new Spline(
                new Vec3d[]{new Vec3d(0, 0, 0), new Vec3d(5, 5, 5), new Vec3d(2, 3, 6), new Vec3d(9, 3, 7)},
                Color.CYAN
        ));
        splines.add(new Spline(
                new Vec3d[]{new Vec3d(10, 10, 10), new Vec3d(15, 15, 15), new Vec3d(12, 13, 16), new Vec3d(19, 13, 17)},
                Color.CYAN
        ));
    }

    private static void drawVertex(VertexConsumer buffer, Matrix4fc matrix, Vec3d pos, Color c) {
        buffer.vertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z)
                .color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha())
                .normal(0f, 1f, 0f).lineWidth(5.0f);
    }

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static MappableRingBuffer vertexBuffer;

    private static void drawLinesThroughWalls(MinecraftClient client, @SuppressWarnings("SameParameterValue") RenderPipeline pipeline) {
        // Build the buffer
        BuiltBuffer builtBuffer = buffer.end();
        BuiltBuffer.DrawParameters drawParameters = builtBuffer.getDrawParameters();
        VertexFormat format = drawParameters.format();

        GpuBuffer vertices = upload(drawParameters, format, builtBuffer);

        draw(client, pipeline, builtBuffer, drawParameters, vertices, format);

        // Rotate the vertex buffer so we are less likely to use buffers that the GPU is using
        vertexBuffer.rotate();
        buffer = null;
    }

    private static GpuBuffer upload(BuiltBuffer.DrawParameters drawParameters, VertexFormat format, BuiltBuffer builtBuffer) {
        // Calculate the size needed for the vertex buffer
        int vertexBufferSize = drawParameters.vertexCount() * format.getVertexSize();

        // Initialize or resize the vertex buffer as needed
        if (vertexBuffer == null || vertexBuffer.size() < vertexBufferSize) {
            if (vertexBuffer != null) {
                vertexBuffer.close();
            }

            vertexBuffer = new MappableRingBuffer(() -> Redstoneeda.MOD_ID + " through wall line render pipeline", GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE, vertexBufferSize);
        }

        // Copy vertex data into the vertex buffer
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(vertexBuffer.getBlocking().slice(0, builtBuffer.getBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(builtBuffer.getBuffer(), mappedView.data());
        }

        return vertexBuffer.getBlocking();
    }

    private static void draw(MinecraftClient client, RenderPipeline pipeline, BuiltBuffer builtBuffer, BuiltBuffer.DrawParameters drawParameters, GpuBuffer vertices, VertexFormat format) {
        GpuBuffer indices;
        VertexFormat.IndexType indexType;

        if (pipeline.getVertexFormatMode() == VertexFormat.DrawMode.QUADS) {
            // Sort the quads if there is translucency
            builtBuffer.sortQuads(allocator, RenderSystem.getProjectionType().getVertexSorter());
            // Upload the index buffer
            indices = pipeline.getVertexFormat().uploadImmediateIndexBuffer(builtBuffer.getSortedBuffer());
            indexType = builtBuffer.getDrawParameters().indexType();
        } else {
            // Use the general shape index buffer for non-quad draw modes
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = shapeIndexBuffer.getIndexBuffer(drawParameters.indexCount());
            indexType = shapeIndexBuffer.getIndexType();
        }

        // Actually execute the draw
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);
        try (RenderPass renderPass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> Redstoneeda.MOD_ID + " through wall line render pipeline rendering", client.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), client.getFramebuffer().getDepthAttachmentView(), OptionalDouble.empty())) {
            renderPass.setPipeline(pipeline);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("DynamicTransforms", dynamicTransforms);

            // Bind texture if applicable:
            // Sampler0 is used for texture inputs in vertices
            // renderPass.bindTexture("Sampler0", textureSetup.texure0(), textureSetup.sampler0());

            renderPass.setVertexBuffer(0, vertices);
            renderPass.setIndexBuffer(indices, indexType);

            // The base vertex is the starting index when we copied the data into the vertex buffer divided by vertex size
            //noinspection ConstantValue
            renderPass.drawIndexed(0 / format.getVertexSize(), 0, drawParameters.indexCount(), 1);
        }

        builtBuffer.close();
    }

    public static void close() {
        allocator.close();

        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}
