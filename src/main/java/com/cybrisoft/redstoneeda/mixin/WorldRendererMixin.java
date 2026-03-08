package com.cybrisoft.redstoneeda.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderMain(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/client/render/Frustum;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;ZLnet/minecraft/client/render/state/WorldRenderState;Lnet/minecraft/client/render/RenderTickCounter;Lnet/minecraft/util/profiler/Profiler;)V", at = @At("HEAD"), cancellable = true)
    public void render(FrameGraphBuilder frameGraphBuilder, Frustum frustum, Matrix4f posMatrix, GpuBufferSlice fogBuffer, boolean renderBlockOutline, WorldRenderState state, RenderTickCounter tickCounter, Profiler profiler, CallbackInfo ci) {
        //ci.cancel();
    }
}
