package com.cybrisoft.redstoneeda.mixin;

import com.cybrisoft.redstoneeda.debugging.ServerEntityTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow
    public abstract float getMaxHealth();

    @Inject(method = "setHealth", at = @At("HEAD"))
    public void redstoneeda$healthChanged(float health, CallbackInfo ci) {
        int id = ((LivingEntity) (Object) this).getId();
        ServerEntityTracker.updateHealth(id,  MathHelper.clamp(health, 0.0F, getMaxHealth()));
    }
}
