package net.pixeldreamstudios.difficultyplus.internal.damage;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.api.ResistanceProvider;
import net.pixeldreamstudios.difficultyplus.internal.level.LevelGap;

public final class LevelGapResistance implements ResistanceProvider {

    @Override
    public Result resistance(LivingEntity target, DamageSource source) {
        if (!(target instanceof ServerPlayer player)) {
            return Result.NONE;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return Result.NONE;
        }

        float multiplier = LevelGap.takenMultiplier(player, attacker);
        if (multiplier == 1f) {
            return Result.NONE;
        }
        return new Result(0f, 1f - multiplier);
    }
}
