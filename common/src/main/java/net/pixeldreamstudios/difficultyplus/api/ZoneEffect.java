package net.pixeldreamstudios.difficultyplus.api;

import net.minecraft.world.entity.LivingEntity;

public interface ZoneEffect {

    void apply(LivingEntity entity, ZoneContext context);

    default void remove(LivingEntity entity, ZoneContext context) {}

    default void tick(LivingEntity entity, ZoneContext context) {}

    default int tickInterval() {
        return 0;
    }
}
