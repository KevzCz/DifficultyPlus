package net.pixeldreamstudios.difficultyplus.internal.level;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.Difficulty;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.api.OwnedLevelSource;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.config.LevelGapData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.DifficultyPlusInternals;
import net.pixeldreamstudios.difficultyplus.internal.effect.TargetMatcher;
import net.pixeldreamstudios.difficultyplus.internal.zone.LevelRoll;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public final class OwnedMobLevels {

    private static final DifficultyPlusData.TargetFilter EVERY_MOB = new DifficultyPlusData.TargetFilter();

    private OwnedMobLevels() {
    }

    @Nullable
    public static PatternMatching.DifficultySearchResult summon(ServerLevel level, PatternMatching.DifficultySearchResult result) {
        LivingEntity entity = ScalingEntity.current();
        if (entity == null) {
            return null;
        }
        OptionalInt summonLevel = summonLevel(entity);
        if (summonLevel.isEmpty()) {
            return null;
        }
        int summoned = summonLevel.getAsInt();
        Difficulty zone = result.difficulty();
        PatternMatching.DifficultySearchResult typed = result.withType(summonType(level, zone.type().name));
        Difficulty difficulty = new Difficulty(typed.difficulty().type(), summoned, summoned, summoned);
        ScaledZoneAccess access = (ScaledZoneAccess) entity;
        access.difficultyplus$setScaledZone(difficulty.type().name);
        access.difficultyplus$setScaledZoneLevel(summoned);
        access.difficultyplus$setScaledZoneMatch(null);
        DifficultyPlus.debug(() -> String.format("summon %s set to level %d as %s (zone %s level %d)",
                entity.getType().getDescriptionId(), summoned, difficulty.type().name, zone.type().name, zone.level()));
        return new PatternMatching.DifficultySearchResult(difficulty, result.locationData(), result.match());
    }

    private static String summonType(ServerLevel level, String zoneType) {
        String configured = LevelGap.config().owned_mobs.difficulty;
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        Config config = ConfigBridge.config();
        if (config == null || config.dimensions == null) {
            return zoneType;
        }
        ResourceLocation dimensionId = level.dimension().location();
        for (Config.Dimension dimension : config.dimensions) {
            if (dimension.world_matches != null && !LevelRoll.matchesDimension(dimensionId, dimension.world_matches.dimension)) {
                continue;
            }
            return dimension.difficulty != null && dimension.difficulty.name != null ? dimension.difficulty.name : zoneType;
        }
        return zoneType;
    }

    private static OptionalInt summonLevel(LivingEntity entity) {
        LevelGapData.OwnedMobs owned = LevelGap.config().owned_mobs;
        if (owned == null || !owned.enabled || !(owner(entity) instanceof ServerPlayer player)
                || !TargetMatcher.matches(owned.targets == null ? EVERY_MOB : owned.targets, entity)) {
            return OptionalInt.empty();
        }
        for (OwnedLevelSource source : DifficultyPlusInternals.ownedLevelSources()) {
            OptionalInt level = source.levelOf(entity);
            if (level.isPresent()) {
                return level;
            }
        }
        return LevelGap.active() ? LevelGap.playerLevel(player) : OptionalInt.empty();
    }

    @Nullable
    private static Entity owner(LivingEntity entity) {
        if (entity instanceof OwnableEntity ownable) {
            return ownable.getOwner();
        }
        return entity instanceof TraceableEntity traceable ? traceable.getOwner() : null;
    }
}
