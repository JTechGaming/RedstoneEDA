package com.cybrisoft.redstoneeda.mixin;

import com.cybrisoft.redstoneeda.Project;
import com.cybrisoft.redstoneeda.managers.ServerDebugManager;
import com.cybrisoft.redstoneeda.util.MathUtils;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongMaps;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.tick.ChunkTickScheduler;
import net.minecraft.world.tick.OrderedTick;
import net.minecraft.world.tick.WorldTickScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Queue;

@Mixin(WorldTickScheduler.class)
public class WorldTickSchedulerMixin<T> {
    /**
     * @author
     * @reason
     */
    @Overwrite
    private void collectTickableChunkTickSchedulers(long time) {
        WorldTickScheduler<T> thisWTS = (WorldTickScheduler<T>) (Object) this;

        ObjectIterator<Long2LongMap.Entry> objectIterator = Long2LongMaps.fastIterator(thisWTS.nextTriggerTickByChunkPos);

        while(objectIterator.hasNext()) {
            Long2LongMap.Entry entry = (Long2LongMap.Entry)objectIterator.next();
            long l = entry.getLongKey();
            long m = entry.getLongValue();
            if (m <= time) {
                ChunkTickScheduler<T> chunkTickScheduler = (ChunkTickScheduler)thisWTS.chunkTickSchedulers.get(l);
                if (chunkTickScheduler == null) {
                    objectIterator.remove();
                } else {
                    OrderedTick<T> orderedTick = chunkTickScheduler.peekNextTick();
                    if (orderedTick == null) {
                        objectIterator.remove();
                    } else if (orderedTick.triggerTick() > time) {
                        entry.setValue(orderedTick.triggerTick());
                    } else if (thisWTS.tickingFutureReadyPredicate.test(l)) {
                        if (isFrozen(orderedTick.pos())) {
                            continue;
                        }

                        objectIterator.remove();
                        thisWTS.tickableChunkTickSchedulers.add(chunkTickScheduler);
                    }
                }
            }
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
}
