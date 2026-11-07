package net.pixeldreamstudios.difficultyplus.internal.spawn;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.DifficultyPlusInternals;
import net.pixeldreamstudios.difficultyplus.internal.effect.TargetMatcher;

import java.util.List;
import java.util.OptionalInt;

public final class SpawnRules {

    private SpawnRules() {
    }

    public static boolean allows(ServerLevel level, Mob mob) {
        List<GlobalData.SpawnRule> rules = ConfigBridge.global().spawn_rules;
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        OptionalInt zoneLevel = null;
        for (GlobalData.SpawnRule rule : rules) {
            if (rule.targets == null || !TargetMatcher.matches(rule.targets, mob)) {
                continue;
            }
            if (zoneLevel == null) {
                zoneLevel = DifficultyPlusInternals.levelAt(level, mob.blockPosition());
            }
            if (!permits(rule, zoneLevel)) {
                return false;
            }
        }
        return true;
    }

    private static boolean permits(GlobalData.SpawnRule rule, OptionalInt zoneLevel) {
        if (zoneLevel.isEmpty()) {
            return rule.allow_outside_zones;
        }
        int value = zoneLevel.getAsInt();
        return value >= rule.min_level && (rule.max_level <= 0 || value <= rule.max_level);
    }
}
