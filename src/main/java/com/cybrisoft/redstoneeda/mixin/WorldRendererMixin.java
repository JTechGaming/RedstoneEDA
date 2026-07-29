package com.cybrisoft.redstoneeda.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
    @Inject(method = "renderBlockLayers(Lorg/joml/Matrix4fc;DDD)Lnet/minecraft/client/render/SectionRenderState;", at = @At("HEAD"), cancellable = true)
    public void render(Matrix4fc matrix, double cameraX, double cameraY, double cameraZ, CallbackInfoReturnable<SectionRenderState> cir) {
        //cir.setReturnValue(new SectionRenderState());
    }
}
