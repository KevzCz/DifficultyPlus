package net.pixeldreamstudios.difficultyplus.internal.level;

import net.dungeon_difficulty.logic.Difficulty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.config.LevelGapData;
import net.pixeldreamstudios.difficultyplus.internal.effect.TargetMatcher;

import java.util.OptionalInt;

public final class OwnedMobLevels {

    private static final DifficultyPlusData.TargetFilter EVERY_MOB = new DifficultyPlusData.TargetFilter();

    private OwnedMobLevels() {
    }

    public static Difficulty match(Difficulty difficulty) {
        LivingEntity entity = ScalingEntity.current();
        if (entity == null || difficulty == null) {
            return difficulty;
        }
        OptionalInt owner = ownerLevel(entity);
        if (owner.isEmpty()) {
            return difficulty;
        }
        int level = owner.getAsInt();
        if (difficulty.level() == level && difficulty.entityLevel() == level && difficulty.rewardLevel() == level) {
            return difficulty;
        }
        ((ScaledZoneAccess) entity).difficultyplus$setScaledZoneLevel(level);
        DifficultyPlus.debug(() -> String.format("owned %s set to owner level %d (zone level %d)",
                entity.getType().getDescriptionId(), level, difficulty.level()));
        return new Difficulty(difficulty.type(), level, level, level);
    }

    public static OptionalInt ownerLevel(LivingEntity entity) {
        LevelGapData.OwnedMobs owned = LevelGap.config().owned_mobs;
        if (!LevelGap.active() || owned == null || !owned.enabled || !(entity instanceof OwnableEntity ownable)
                || !(ownable.getOwner() instanceof ServerPlayer player)
                || !TargetMatcher.matches(owned.targets == null ? EVERY_MOB : owned.targets, entity)) {
            return OptionalInt.empty();
        }
        return LevelGap.playerLevel(player);
    }
}
