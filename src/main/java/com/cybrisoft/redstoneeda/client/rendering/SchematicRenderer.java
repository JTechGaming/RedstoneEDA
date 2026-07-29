package com.cybrisoft.redstoneeda.client.rendering;

import com.cybrisoft.redstoneeda.Redstoneeda;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.DynamicUniforms;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

import java.util.*;

public class SchematicRenderer {
    private static final RenderPipeline SCHEMATIC_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.BLOCK_SNIPPET)
                    .withLocation(Redstoneeda.identifier("pipeline/schematic"))
                    .build()
    );

    private static SchematicRenderer instance;
    public static SchematicRenderer getInstance() { return instance; }

    // -------------------------------------------------------------------------

    public static class Schematic {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();

        public void setBlock(BlockPos pos, BlockState state) { blocks.put(pos, state); }
        public BlockState getBlock(BlockPos pos) { return blocks.getOrDefault(pos, Blocks.AIR.getDefaultState()); }
        public Set<Map.Entry<BlockPos, BlockState>> getBlocks() { return blocks.entrySet(); }
    }

    // -------------------------------------------------------------------------

    public static final class ChunkPos3D {
        public final int x, y, z;

        public ChunkPos3D(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

        public static ChunkPos3D fromBlockPos(BlockPos pos) {
            return new ChunkPos3D(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        }

        public BlockPos getOrigin() { return new BlockPos(x << 4, y << 4, z << 4); }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ChunkPos3D c)) return false;
            return x == c.x && y == c.y && z == c.z;
        }
        @Override public int hashCode() { return Objects.hash(x, y, z); }
    }

    // -------------------------------------------------------------------------

    public static class SchematicRenderChunk {
        private final ChunkPos3D chunkPos;
        private GpuBuffer vertexBuffer;
        private GpuBuffer indexBuffer;
        private VertexFormat.IndexType indexType;
        private int indexCount;
        private boolean compiled = false;

        public SchematicRenderChunk(ChunkPos3D pos) { this.chunkPos = pos; }

        public void rebuild(Schematic schematic) {
            close();

            MinecraftClient client = MinecraftClient.getInstance();
            BlockRenderManager brm = client.getBlockRenderManager();
            if (brm == null) return;

            BlockPos origin = chunkPos.getOrigin();
            BufferAllocator allocator = new BufferAllocator(RenderLayer.field_64009);
            BufferBuilder builder = new BufferBuilder(
                    allocator,
                    SCHEMATIC_PIPELINE.getVertexFormatMode(),
                    SCHEMATIC_PIPELINE.getVertexFormat()
            );

            MatrixStack matrices = new MatrixStack();
            Random random = Random.create();

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos pos = origin.add(x, y, z);
                        BlockState state = schematic.getBlock(pos);
                        if (state.isAir()) continue;

                        matrices.push();
                        matrices.translate(x, y, z);

                        BlockStateModel model = brm.getModel(state);
                        List<BlockModelPart> parts = new ArrayList<>();
                        model.addParts(random, parts);

                        brm.getModelRenderer().render(
                                client.world,
                                parts,
                                state,
                                pos,
                                matrices,
                                builder,
                                false,
                                OverlayTexture.DEFAULT_UV
                        );

                        matrices.pop();
                    }
                }
            }

            BuiltBuffer builtBuffer = builder.endNullable();
            if (builtBuffer == null) {
                allocator.close();
                return;
            }

            BuiltBuffer.DrawParameters drawParams = builtBuffer.getDrawParameters();
            VertexFormat format = drawParams.format();

            // Upload vertices directly to a static GpuBuffer — same as vanilla BuiltChunk
            int vertexSize = drawParams.vertexCount() * format.getVertexSize();
            vertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "schematic_vertices",
                    GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                    vertexSize
            );
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(vertexBuffer.slice(), builtBuffer.getBuffer().limit(vertexSize));

            // Build quad index buffer — sortQuads populates getSortedBuffer()
            BufferAllocator indexAllocator = new BufferAllocator(RenderLayer.field_64009);
            builtBuffer.sortQuads(indexAllocator, RenderSystem.getProjectionType().getVertexSorter());
            var sortedBuf = builtBuffer.getSortedBuffer();
            if (sortedBuf != null) {
                indexBuffer = RenderSystem.getDevice().createBuffer(
                        () -> "schematic_indices",
                        GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                        sortedBuf.remaining()
                );
                RenderSystem.getDevice().createCommandEncoder()
                        .writeToBuffer(indexBuffer.slice(), sortedBuf);
                indexType = drawParams.indexType();
            }
            indexCount = drawParams.indexCount();

            builtBuffer.close();
            allocator.close();
            indexAllocator.close();

            compiled = true;
        }

        public void render(Matrix4fc posMatrix) {
            if (!compiled || vertexBuffer == null) return;

            BlockPos origin = chunkPos.getOrigin();

            // Get atlas texture + dimensions, exactly like vanilla renderBlockLayers
            var atlasTexture = MinecraftClient.getInstance()
                    .getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE);
            var atlasView = atlasTexture.getGlTextureView();
            int atlasW = atlasView.getWidth(0);
            int atlasH = atlasView.getHeight(0);

            // ChunkSectionsValue: posMatrix, chunkOrigin XYZ, animationTime, atlasW, atlasH
            // This is exactly what vanilla passes per built chunk
            DynamicUniforms.ChunkSectionsValue sectionValue = new DynamicUniforms.ChunkSectionsValue(
                    new Matrix4f(posMatrix),
                    origin.getX(),
                    origin.getY(),
                    origin.getZ(),
                    0L, // animation time — 0 is fine for static schematics
                    atlasW,
                    atlasH
            );

            GpuBufferSlice[] chunkUniforms = RenderSystem.getDynamicUniforms()
                    .writeChunkSections(new DynamicUniforms.ChunkSectionsValue[]{ sectionValue });

            // Resolve index buffer: use uploaded static one, or fall back to sequential
            GpuBuffer indices;
            VertexFormat.IndexType resolvedIndexType;
            if (indexBuffer != null) {
                indices = indexBuffer;
                resolvedIndexType = indexType;
            } else {
                RenderSystem.ShapeIndexBuffer seq = RenderSystem.getSequentialBuffer(
                        SCHEMATIC_PIPELINE.getVertexFormatMode());
                indices = seq.getIndexBuffer(indexCount);
                resolvedIndexType = seq.getIndexType();
            }

            MinecraftClient client = MinecraftClient.getInstance();
            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(
                            () -> "schematic_render",
                            client.getFramebuffer().getColorAttachmentView(),
                            OptionalInt.empty(),
                            client.getFramebuffer().getDepthAttachmentView(),
                            OptionalDouble.empty()
                    )) {
                renderPass.setPipeline(SCHEMATIC_PIPELINE);
                RenderSystem.bindDefaultUniforms(renderPass);

                // Upload ChunkSection uniform exactly like vanilla
                renderPass.setUniform("ChunkSection", chunkUniforms[0]);

                renderPass.bindTexture("Sampler0", atlasView, atlasTexture.getSampler());

                renderPass.setVertexBuffer(0, vertexBuffer);
                renderPass.setIndexBuffer(indices, resolvedIndexType);
                renderPass.drawIndexed(0, 0, indexCount, 1);
            }
        }

        public void close() {
            if (vertexBuffer != null) { vertexBuffer.close(); vertexBuffer = null; }
            if (indexBuffer != null)  { indexBuffer.close();  indexBuffer = null;  }
            compiled = false;
        }
    }

    // -------------------------------------------------------------------------

    public static class SchematicChunkBuilder {
        public static Map<ChunkPos3D, List<BlockPos>> buildChunkMap(Schematic schematic) {
            Map<ChunkPos3D, List<BlockPos>> map = new HashMap<>();
            for (var entry : schematic.getBlocks()) {
                ChunkPos3D chunk = ChunkPos3D.fromBlockPos(entry.getKey());
                map.computeIfAbsent(chunk, k -> new ArrayList<>()).add(entry.getKey());
            }
            return map;
        }
    }

    // -------------------------------------------------------------------------

    private final Map<ChunkPos3D, SchematicRenderChunk> chunks = new HashMap<>();

    public void setSchematic(Schematic schematic) {
        chunks.values().forEach(SchematicRenderChunk::close);
        chunks.clear();
        for (ChunkPos3D pos : SchematicChunkBuilder.buildChunkMap(schematic).keySet()) {
            SchematicRenderChunk chunk = new SchematicRenderChunk(pos);
            chunk.rebuild(schematic);
            chunks.put(pos, chunk);
        }
    }

    public static void init() {
        instance = new SchematicRenderer();
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx -> instance.render(ctx));
    }

    public void render(WorldRenderContext ctx) {
        Matrix4f posMatrix = new Matrix4f(RenderSystem.getModelViewStack());

        for (SchematicRenderChunk chunk : chunks.values()) {
            chunk.render(posMatrix);
        }
    }
}