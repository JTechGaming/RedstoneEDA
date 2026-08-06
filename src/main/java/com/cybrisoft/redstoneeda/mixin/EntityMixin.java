package com.cybrisoft.redstoneeda.mixin;

import com.cybrisoft.redstoneeda.debugging.ServerEntityTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Shadow
    private int id;

    public EntityMixin() {
        System.out.println("Creating Entity");
        System.out.println(id);

        ServerEntityTracker.register(new ServerEntityTracker.Entry(id));
    }

    @Inject(method = "onRemove", at = @At("HEAD"))
    public void redstoneeda$removed(Entity.RemovalReason reason, CallbackInfo ci) {
        ServerEntityTracker.remove(id);
    }

    @Inject(method = "setPos", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;<init>(DDD)V"))
    public void redstoneeda$posChanged(double x, double y, double z, CallbackInfo ci) {
        ServerEntityTracker.updatePos(id, x, y, z);
    }

    @Inject(method = "setYaw", at = @At("HEAD"))
    public void redstoneeda$yawChanged(float yaw, CallbackInfo ci) {
        if (Float.isFinite(yaw)) {
            ServerEntityTracker.updateYaw(id, yaw);
        }
    }

    @Inject(method = "setPitch", at = @At("HEAD"))
    public void redstoneeda$pitchChanged(float pitch, CallbackInfo ci) {
        if (Float.isFinite(pitch)) {
            ServerEntityTracker.updatePitch(id, Math.clamp(pitch % 360.0F, -90.0F, 90.0F));
        }
    }
}
