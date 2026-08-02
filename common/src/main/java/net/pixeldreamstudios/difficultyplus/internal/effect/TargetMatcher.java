package net.pixeldreamstudios.difficultyplus.internal.effect;

import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import java.util.Locale;

public final class TargetMatcher {
    private TargetMatcher() {}

    public static boolean matches(DifficultyPlusData.TargetFilter filter, LivingEntity entity) {

        if (filter == null) {
            return entity instanceof Player;
        }
        if (!matchesAttitude(filter.attitude, entity)) {
            return false;
        }
        String pattern = filter.type;
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        var entry = BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType());
        return PatternMatching.universalMatch(entry, Registries.ENTITY_TYPE, pattern);
    }

    public static boolean matchesResistance(DifficultyPlusData.TargetFilter filter, LivingEntity entity) {
        return filter == null || matches(filter, entity);
    }

    private static boolean matchesAttitude(String attitude, LivingEntity entity) {
        if (attitude == null || attitude.isEmpty()) {
            return true;
        }
        boolean hostile = entity instanceof Enemy;
        return switch (attitude.toUpperCase(Locale.ROOT)) {
            case "HOSTILE" -> hostile;
            case "FRIENDLY" -> !hostile;
            default -> true;
        };
    }

    public static boolean targetsNonPlayers(DifficultyPlusData.TargetFilter filter) {
        return filter != null;
    }
}
