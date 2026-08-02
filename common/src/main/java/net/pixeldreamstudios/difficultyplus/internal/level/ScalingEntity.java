package net.pixeldreamstudios.difficultyplus.internal.level;

import net.minecraft.world.entity.LivingEntity;

public final class ScalingEntity {
    private ScalingEntity() {}

    private static final ThreadLocal<LivingEntity> CURRENT = new ThreadLocal<>();

    public static void push(LivingEntity entity) {
        CURRENT.set(entity);
    }

    public static void pop() {
        CURRENT.remove();
    }

    public static LivingEntity current() {
        return CURRENT.get();
    }
}
