package com.cybrisoft.redstoneeda.mixin.compat;

import com.moulberry.mixinconstraints.annotations.IfModLoaded;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@IfModLoaded("malilib")
@Pseudo
@Mixin(targets = "fi.dy.masa.malilib.event.InputEventHandler")
public class MalilibCompatMixin {
//    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true)
//    private static void onKeyInput(int keyCode, int scanCode, int modifiers, int action, MinecraftClient mc, CallbackInfoReturnable<Boolean> cir) {
//        if (ImGuiImplementation.isActiveInternal()) {
//            cir.setReturnValue(false);
//        }
//    }
}
