package com.cybrisoft.redstoneeda.mixin;

import com.cybrisoft.redstoneeda.client.imgui.ImGuiImplementation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    public void initImGui(RunArgs args, CallbackInfo ci) {
        ImGuiImplementation.create();
    }

    @Inject(method = "close", at = @At("RETURN"))
    public void closeImGui(CallbackInfo ci) {
        ImGuiImplementation.dispose();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;blitToScreen()V", shift = At.Shift.AFTER))
    public void afterMainBlit(boolean bl, CallbackInfo ci) {
        if (RenderSystem.isOnRenderThread()) {
            ImGuiImplementation.draw();
        }
    }
}