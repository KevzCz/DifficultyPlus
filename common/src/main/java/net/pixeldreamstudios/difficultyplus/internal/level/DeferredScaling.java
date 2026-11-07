package net.pixeldreamstudios.difficultyplus.internal.level;

import net.dungeon_difficulty.logic.EntityScaling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.WeakHashMap;

public final class DeferredScaling {

    private static final int MAX_PER_TICK = 32;
    private static final Map<ServerLevel, Deque<Entity>> WAITING = new WeakHashMap<>();

    private DeferredScaling() {
    }

    public static boolean deferIfNested(Entity entity, ServerLevel level) {
        if (ScalingEntity.current() == null || !(entity instanceof LivingEntity)) {
            return false;
        }
        WAITING.computeIfAbsent(level, ignored -> new ArrayDeque<>()).add(entity);
        return true;
    }

    public static void drain(ServerLevel level) {
        ScalingEntity.pop();
        Deque<Entity> waiting = WAITING.get(level);
        if (waiting == null) {
            return;
        }
        for (int i = 0; i < MAX_PER_TICK && !waiting.isEmpty(); i++) {
            Entity entity = waiting.poll();
            if (!entity.isRemoved()) {
                EntityScaling.scale(entity, level);
            }
        }
    }
}
