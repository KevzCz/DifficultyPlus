package net.pixeldreamstudios.difficultyplus.internal.level;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.compat.PuffishSkills;
import net.pixeldreamstudios.difficultyplus.config.LevelGapData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.DifficultyPlusInternals;

import java.util.Optional;
import java.util.OptionalInt;

public final class LevelGap {
    private LevelGap() {}

    public static LevelGapData config() {
        LevelGapData data = ConfigBridge.global().level_gap;
        return data == null ? new LevelGapData() : data;
    }

    public static boolean active() {
        LevelGapData data = config();
        return data.enabled && PuffishSkills.isLoaded();
    }

    public static OptionalInt playerLevel(ServerPlayer player) {
        LevelGapData data = config();
        if (!data.enabled) {
            return OptionalInt.empty();
        }
        OptionalInt raw = PuffishSkills.totalLevel(player, data.categories);
        if (raw.isEmpty()) {
            return raw;
        }
        float scale = data.player_scale <= 0f ? 1f : data.player_scale;
        return OptionalInt.of(Math.round(raw.getAsInt() / scale));
    }

    public static OptionalInt mobLevel(LivingEntity mob) {
        if (mob == null || mob instanceof Player) {
            return OptionalInt.empty();
        }
        return DifficultyPlusInternals.scaledZoneOf(mob)
                .map(DifficultyPlusAPI.ScaledZone::level)
                .filter(level -> level > 0)
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

    public static OptionalInt gap(ServerPlayer viewer, LivingEntity mob) {
        if (!active()) {
            return OptionalInt.empty();
        }
        OptionalInt player = playerLevel(viewer);
        OptionalInt target = mobLevel(mob);
        if (player.isEmpty() || target.isEmpty()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(player.getAsInt() - target.getAsInt());
    }

    public static Optional<LevelGapData.Threat> threat(ServerPlayer viewer, LivingEntity mob) {
        LevelGapData data = config();
        if (!data.enabled) {
            return Optional.empty();
        }
        OptionalInt gap = gap(viewer, mob);
        return gap.isEmpty() ? Optional.empty() : Optional.of(threatOf(gap.getAsInt(), data));
    }

    public static LevelGapData.Threat threatOf(int gap, LevelGapData data) {
        if (gap <= -Math.max(1, data.red_gap)) {
            return LevelGapData.Threat.RED;
        }
        if (gap <= -Math.max(1, data.orange_gap)) {
            return LevelGapData.Threat.ORANGE;
        }
        if (gap >= Math.max(1, data.green_gap)) {
            return LevelGapData.Threat.GREEN;
        }
        return LevelGapData.Threat.WHITE;
    }

    public static boolean mobVsMobActive() {
        LevelGapData data = config();
        return data.enabled && data.mob_vs_mob != null && data.mob_vs_mob.enabled;
    }

    public static float mobVsMobMultiplier(LivingEntity attacker, LivingEntity victim) {
        if (!mobVsMobActive()) {
            return 1f;
        }
        OptionalInt attackerLevel = mobLevel(attacker);
        OptionalInt victimLevel = mobLevel(victim);
        if (attackerLevel.isEmpty() || victimLevel.isEmpty()) {
            return 1f;
        }

        int value = attackerLevel.getAsInt() - victimLevel.getAsInt();
        if (value == 0) {
            return 1f;
        }

        LevelGapData.MobVsMob settings = config().mob_vs_mob;
        LevelGapData.Half half = value < 0 ? settings.attacker_below : settings.attacker_above;
        if (half == null || !half.isActive()) {
            return 1f;
        }

        float rate = half.dealt_per_level;
        if (rate == 0f) {
            return 1f;
        }

        float factor = (float) (Math.log1p(Math.abs(value)) * rate);
        return half.clamp(value > 0 ? 1f + factor : 1f - factor);
    }

    public static float dealtMultiplier(ServerPlayer attacker, LivingEntity mob) {
        return multiplier(attacker, mob, true);
    }

    public static float takenMultiplier(ServerPlayer victim, LivingEntity mob) {
        return multiplier(victim, mob, false);
    }

    private static float multiplier(ServerPlayer player, LivingEntity mob, boolean dealt) {
        OptionalInt gap = gap(player, mob);
        if (gap.isEmpty() || gap.getAsInt() == 0) {
            return 1f;
        }

        LevelGapData data = config();
        int value = gap.getAsInt();
        LevelGapData.Half half = value < 0 ? data.player_below : data.player_above;
        if (half == null || !half.isActive()) {
            return 1f;
        }

        float rate = dealt ? half.dealt_per_level : half.taken_per_level;
        if (rate == 0f) {
            return 1f;
        }

        float factor = (float) (Math.log1p(Math.abs(value)) * rate);
        boolean favoursPlayer = value > 0;
        float multiplier = dealt
                ? (favoursPlayer ? 1f + factor : 1f - factor)
                : (favoursPlayer ? 1f - factor : 1f + factor);
        return half.clamp(multiplier);
    }
}
