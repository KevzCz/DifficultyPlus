package net.pixeldreamstudios.difficultyplus.internal.damage;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusEvents;
import net.pixeldreamstudios.difficultyplus.api.ResistanceProvider;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.DifficultyPlusInternals;

public final class ResistanceEngine {
    private ResistanceEngine() {}

    public static float apply(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0f || target == null || source == null) {
            return amount;
        }
        float reduced = reduce(target, source, amount);
        if (reduced != amount && DifficultyPlusEvents.DAMAGE_RESISTED.isListened()) {
            final float before = amount;
            final float after = reduced;
            DifficultyPlusEvents.DAMAGE_RESISTED.invoke(l -> l.onResisted(target, source, before, after));
        }
        return reduced;
    }

    private static float reduce(LivingEntity target, DamageSource source, float amount) {
        float flat = 0f;
        float remaining = 1f;

        for (ResistanceProvider provider : DifficultyPlusInternals.resistanceProviders()) {
            ResistanceProvider.Result result = provider.resistance(target, source);
            if (result == null || result.isNone()) {
                continue;
            }
            flat += result.flat();

            remaining *= (1f - result.percent());
        }

        float reduction = 1f - remaining;
        float cap = ConfigBridge.global().resistance_reduction_cap;
        if (reduction > cap) {
            reduction = cap;
        }

        float result = Math.max(0f, amount - flat) * (1f - reduction);
        return Math.max(0f, result);
    }

    public static float totalReduction(LivingEntity target, DamageSource source) {
        float before = 100f;
        return 1f - (reduce(target, source, before) / before);
    }
}
