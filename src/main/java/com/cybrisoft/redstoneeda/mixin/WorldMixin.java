package com.cybrisoft.redstoneeda.mixin;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.managers.ServerDebugManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
    @Inject(
            method = "shouldTickBlockPos",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void redstoneeda$freezeBlocks(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        for (Project project : ServerDebugManager.getCurrentSessions().values()) { // this is probably fine for sp as well bc the list of sessions should be empty there
            if (!project.isFrozen()) continue;
            if (project.getMin() == null) continue;

            if (intersects(pos, project.getMin(), project.getMax())) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    @Unique
    boolean intersects(BlockPos pos, BlockPos p1, BlockPos p2) {
        return pos.getX() >= Math.min(p1.getX(), p2.getX()) && pos.getX() <= Math.max(p1.getX(), p2.getX());
    }
}
