package net.pixeldreamstudios.difficultyplus.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

@FunctionalInterface
public interface ResistanceProvider {

    Result resistance(LivingEntity target, DamageSource source);

    record Result(float flat, float percent) {
        public static final Result NONE = new Result(0f, 0f);

        public boolean isNone() {
            return flat == 0f && percent == 0f;
        }
    }
}
