package net.pixeldreamstudios.difficultyplus.internal.zone;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.Difficulty;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.api.StructureKey;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigAccess;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.level.ScaledZoneAccess;
import net.pixeldreamstudios.difficultyplus.internal.level.ScalingEntity;

import java.util.Optional;

public final class RolledLevel {
    private RolledLevel() {}

    public static Difficulty apply(ServerLevel level,
                                   PatternMatching.LocationData location,
                                   PatternMatching.DifficultySearchResult result,
                                   Difficulty difficulty) {
        if (location == null) {
            return difficulty;
        }
        BlockPos pos = location.position();
        if (pos == null) {
            return difficulty;
        }

        int rolled;
        Config.DifficultyReference reference;

        Config.EntityMatcher matcher = matchedEntity(level, difficulty);
        if (matcher != null) {
            rolled = entityRoll(level, matcher, difficulty.level());
            reference = matcher.difficulty;
        } else {
            ResourceLocation matchedId = result.matchId();
            if (matchedId == null) {
                return difficulty;
            }
            Optional<StructureKey> key = StructureLookup.keyAt(level, pos, matchedId);
            rolled = LevelRoll.resolve(
                    level, pos, difficulty.type().name, difficulty.level(), key, matchedId);

            reference = referenceFor(level, pos, matchedId);
        }

        recordOrigin(difficulty.type().name, rolled, result.matchId());

        if (rolled == difficulty.level()) {
            DifficultyPlus.debug(() -> String.format(
                    "roll %s @ %d,%d,%d  type=%s  configured=%d  rolled=%d  (unchanged)  match=%s",
                    goalOf(result), pos.getX(), pos.getY(), pos.getZ(),
                    difficulty.type().name, difficulty.level(), difficulty.level(),
                    result.matchId()));
            return difficulty;
        }

        int entityLevel = derive(reference == null ? null : reference.entity_level,
                difficulty.level(), difficulty.entityLevel(), rolled);
        int rewardLevel = derive(reference == null ? null : reference.reward_level,
                difficulty.level(), difficulty.rewardLevel(), rolled);

        final int finalRolled = rolled;
        final Config.DifficultyReference finalReference = reference;
        DifficultyPlus.debug(() -> String.format(
                "roll %s @ %d,%d,%d  type=%s  configured=%d -> rolled=%d  entity=%d reward=%d"
                        + "  match=%s  entityEntry=%s  pinned(e=%s,r=%s)",
                goalOf(result), pos.getX(), pos.getY(), pos.getZ(),
                difficulty.type().name, difficulty.level(), finalRolled,
                entityLevel, rewardLevel, result.matchId(),
                matcher != null,
                finalReference == null ? "-" : String.valueOf(finalReference.entity_level),
                finalReference == null ? "-" : String.valueOf(finalReference.reward_level)));

        return new Difficulty(difficulty.type(), rolled, entityLevel, rewardLevel);
    }

    public static int preview(ServerLevel level,
                              PatternMatching.DifficultySearchResult result) {
        if (result == null) {
            return Integer.MIN_VALUE;
        }
        Difficulty difficulty = result.difficulty();
        if (difficulty == null || !difficulty.isValid()) {
            return Integer.MIN_VALUE;
        }
        PatternMatching.LocationData location = result.locationData();
        if (location == null || location.position() == null) {
            return difficulty.level();
        }
        BlockPos pos = location.position();

        Config.EntityMatcher matcher = matchedEntity(level, difficulty);
        if (matcher != null) {
            return entityRoll(level, matcher, difficulty.level());
        }

        ResourceLocation matchedId = result.matchId();
        if (matchedId == null) {
            return difficulty.level();
        }
        Optional<StructureKey> key = StructureLookup.keyAt(level, pos, matchedId);
        return LevelRoll.resolve(
                level, pos, difficulty.type().name, difficulty.level(), key, matchedId);
    }

    private static String goalOf(PatternMatching.DifficultySearchResult result) {
        return ScalingEntity.current() != null ? "ENTITY" : "OTHER";
    }

    private static void recordOrigin(String difficultyName, int level, ResourceLocation matchedId) {
        LivingEntity entity = ScalingEntity.current();
        if (entity == null) {
            return;
        }
        ScaledZoneAccess access = (ScaledZoneAccess) entity;
        access.difficultyplus$setScaledZone(difficultyName);
        access.difficultyplus$setScaledZoneLevel(level);
        access.difficultyplus$setScaledZoneMatch(matchedId == null ? null : matchedId.toString());
    }

    private static Config.EntityMatcher matchedEntity(ServerLevel level, Difficulty difficulty) {
        LivingEntity entity = ScalingEntity.current();
        if (entity == null) {
            return null;
        }
        Config config = ConfigBridge.config();
        if (config == null || config.dimensions == null) {
            return null;
        }

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        ResourceLocation dimensionId = level.dimension().location();

        for (Config.Dimension dimension : config.dimensions) {
            if (dimension.world_matches != null
                    && !LevelRoll.matchesDimension(dimensionId, dimension.world_matches.dimension)) {
                continue;
            }
            if (dimension.entities == null) {
                continue;
            }
            for (Config.EntityMatcher matcher : dimension.entities) {
                if (matcher.difficulty == null
                        || !difficulty.type().name.equals(matcher.difficulty.name)
                        || matcher.difficulty.level != difficulty.level()) {
                    continue;
                }
                ZoneData data = ((ConfigAccess.Entity) (Object) matcher).difficultyplus$entity();
                if (data.level_range == null || !data.level_range.isRanged()) {
                    continue;
                }

                if (matcher.entity_type != null && !matcher.entity_type.isEmpty()
                        && PatternMatching.idMatches(entityId, matcher.entity_type)) {
                    return matcher;
                }
            }
        }
        return null;
    }

    private static int entityRoll(ServerLevel level, Config.EntityMatcher matcher, int configuredLevel) {
        ZoneData.LevelRange range = ((ConfigAccess.Entity) (Object) matcher)
                .difficultyplus$entity().level_range;
        int min = range.lowestFor(configuredLevel);
        int max = range.highestFor(configuredLevel);
        if (max <= min) {
            return configuredLevel;
        }
        LivingEntity entity = ScalingEntity.current();
        if (entity == null) {
            return configuredLevel;
        }
        return LevelRoll.hashedEntityLevel(level.getSeed(), entity.getUUID(), min, max);
    }

    private static int derive(Integer pinned, int configuredLevel, int resolved, int rolled) {
        if (pinned == null) {
            return rolled;
        }
        return resolved + (rolled - configuredLevel);
    }

    private static Config.DifficultyReference referenceFor(ServerLevel level,
                                                           BlockPos pos,
                                                           ResourceLocation matchedId) {
        Config config = ConfigBridge.config();
        if (config == null || config.dimensions == null) {
            return null;
        }
        ResourceLocation dimensionId = level.dimension().location();
        for (Config.Dimension dimension : config.dimensions) {
            if (dimension.world_matches != null
                    && !LevelRoll.matchesDimension(dimensionId, dimension.world_matches.dimension)) {
                continue;
            }
            if (dimension.zones == null) {
                continue;
            }
            for (Config.Zone zone : dimension.zones) {
                ZoneData data = ConfigBridge.forZone(zone);
                if (data.level_range == null || !data.level_range.isRanged()) {
                    continue;
                }
                if (LevelRoll.zoneMentions(level, pos, zone, matchedId)) {
                    return zone.difficulty;
                }
            }
        }
        return null;
    }
}
