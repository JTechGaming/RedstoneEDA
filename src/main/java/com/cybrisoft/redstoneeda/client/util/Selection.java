package com.cybrisoft.redstoneeda.client.util;

import com.cybrisoft.redstoneeda.client.RedstoneedaClient;
import com.cybrisoft.redstoneeda.client.rendering.OutlineRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;

public class Selection {
    private boolean pos1Set = false;
    private boolean pos2Set = false;
    private boolean complete = false;
    private BlockPos pos1;
    private BlockPos pos2;
    private CompletionAction completionAction;

    public Selection(CompletionAction completionAction) {
        this.completionAction = completionAction;
    }

    public void tick() {
        if (pos1Set && !pos2Set) {
            // render outline from pos1 to current mouse position
            BlockPos currentPos = MathHelper.performRaycast(MinecraftClient.getInstance(), 100);
            if (currentPos != null) {
                OutlineRenderer.outlines.put(pos1, currentPos);
            }
        } else if (pos1Set && pos2Set) {
            // render outline from pos1 to pos2
            OutlineRenderer.outlines.put(pos1, pos2);
        }
        if (RedstoneedaClient.isRightClicking) {
            if (pos1Set) {
                OutlineRenderer.outlines.remove(pos1);
            }
            BlockPos raycastPos = MathHelper.performRaycast(MinecraftClient.getInstance(), 150);
            if (raycastPos != null) {
                pos1 = raycastPos;
                pos1Set = true;
            }
        }
        if (RedstoneedaClient.isLeftClicking) {
            BlockPos raycastPos = MathHelper.performRaycast(MinecraftClient.getInstance(), 150);
            if (raycastPos != null) {
                pos2 = raycastPos;
                pos2Set = true;
            }
        }
        if (InputUtil.isKeyPressed(MinecraftClient.getInstance().getWindow(), InputUtil.GLFW_KEY_ENTER) && pos1Set && pos2Set) {
            finishSelection();
            complete = true;
            OutlineRenderer.outlines.remove(pos1, pos2);
            completionAction.onSelectionComplete(pos1, pos2);
        }
    }

    public void finishSelection() {
        pos1Set = false;
        pos2Set = false;
    }

    public boolean isComplete() {
        return complete;
    }

    @FunctionalInterface
    public interface CompletionAction {
        void onSelectionComplete(BlockPos pos1, BlockPos pos2);
    }
}
