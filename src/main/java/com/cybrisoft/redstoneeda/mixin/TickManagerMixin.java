package com.cybrisoft.redstoneeda.mixin;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.managers.ServerDebugManager;
import com.cybrisoft.redstoneeda.util.MathUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.tick.TickManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TickManager.class)
public class TickManagerMixin {
    @Inject(method = "shouldSkipTick", at = @At("HEAD"), cancellable = true)
    public void shouldSkipEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ServerDebugManager.getCurrentSessions().isEmpty()) return;

        if (entity instanceof PlayerEntity) return;

        for (Project project : ServerDebugManager.getCurrentSessions().values()) {
            if (!project.isFrozen()) continue;
            if (project.getMin() == null) continue;
            if (MathUtils.intersects(entity.getBlockPos(), project.getMin(), project.getMax())) {
                cir.setReturnValue(true); //todo check if frozen
            }
        }
    }
}
