package com.cybrisoft.redstoneeda.mixin;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.managers.ServerDebugManager;
import com.cybrisoft.redstoneeda.util.FrozenNeighborUpdater;
import com.cybrisoft.redstoneeda.util.MathUtils;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.block.ChainRestrictedNeighborUpdater;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;

@Mixin(ChainRestrictedNeighborUpdater.class)
public abstract class ChainRestrictedNeighborUpdaterMixin implements FrozenNeighborUpdater {
    @Shadow
    protected abstract void enqueue(BlockPos pos, ChainRestrictedNeighborUpdater.Entry entry);

    @Unique
    private final ArrayDeque<Pair<BlockPos, ChainRestrictedNeighborUpdater.Entry>> redstoneeda$frozenUpdates = new ArrayDeque<>();

    @Inject(method = "enqueue", at = @At(value = "HEAD"), cancellable = true)
    private void enqueue(BlockPos pos, ChainRestrictedNeighborUpdater.Entry entry, CallbackInfo ci) {
        if (isFrozen(pos)) {
            redstoneeda$frozenUpdates.add(new Pair<>(pos, entry));
            ci.cancel();
        }
    }

    @Unique
    private boolean isFrozen(BlockPos pos) {
        if (ServerDebugManager.getCurrentSessions().isEmpty()) return false;

        for (Project project : ServerDebugManager.getCurrentSessions().values()) {
            if (!project.isFrozen()) continue;
            if (project.getMin() == null) continue;
            if (MathUtils.intersects(pos, project.getMin(), project.getMax())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void redstoneeda$unfreeze() {
        ArrayDeque<Pair<BlockPos, ChainRestrictedNeighborUpdater.Entry>> temp = new ArrayDeque<>();

        while (!redstoneeda$frozenUpdates.isEmpty()) {
            Pair<BlockPos, ChainRestrictedNeighborUpdater.Entry> entry = redstoneeda$frozenUpdates.poll();
            if (isFrozen(entry.getLeft())) { // if this position is still frozen, there is still (another) debug session frozen that encapsulates this position
                temp.add(entry);
                continue;
            }
            enqueue(entry.getLeft(), entry.getRight());
        }

        // redstoneeda$frozenUpdates.clear(); // if it gets here it should always be empty so this is prob not needed
        redstoneeda$frozenUpdates.addAll(temp);
    }
}
