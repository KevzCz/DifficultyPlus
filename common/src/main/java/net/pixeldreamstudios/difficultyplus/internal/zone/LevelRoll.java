package net.pixeldreamstudios.difficultyplus.internal.zone;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.internal.level.StructureLevelAccess;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusEvents;
import net.pixeldreamstudios.difficultyplus.api.LevelResolver;
import net.pixeldreamstudios.difficultyplus.api.StructureContext;
import net.pixeldreamstudios.difficultyplus.api.StructureKey;
import net.pixeldreamstudios.difficultyplus.config.DimensionData;
import net.pixeldreamstudios.difficultyplus.config.DistanceScaling;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.DifficultyPlusInternals;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class LevelRoll {
    private LevelRoll() {}

    private static final String ANY = PatternMatching.ANY;
    private static final String TAG_PREFIX = PatternMatching.TAG_PREFIX;
    private static final String NEGATE_PREFIX = PatternMatching.NEGATE_PREFIX;

    public static boolean isRerollable(ServerLevel level, BlockPos pos,
                                       ResourceLocation matchedId) {
        ZoneData.LevelRange range = rangeFor(level, pos, matchedId);
        return range != null && range.isRanged();
    }

    public static int resolve(ServerLevel level,
                              BlockPos pos,
                              String difficultyName,
                              int configuredLevel,
                              Optional<StructureKey> key,
                              ResourceLocation matchedId) {
        Optional<StructureStart> start = StructureLookup.startAt(level, pos);
        int bias = start.map(s -> ((StructureLevelAccess) (Object) s).difficultyplus$bias())
                .orElse(0);
        int pierce = start.map(s -> ((StructureLevelAccess) (Object) s).difficultyplus$pierce())
                .orElse(0);

        int distance = distanceBonus(level, pos, matchedId);

        ZoneData.LevelRange range = rangeFor(level, pos, matchedId);
        if (range == null || !range.isRanged()) {
            return Math.max(0, configuredLevel + distance);
        }

        int min = range.lowestFor(configuredLevel, (pierce & 1) != 0, (pierce & 2) != 0);
        int max = range.highestFor(configuredLevel, (pierce & 1) != 0, (pierce & 2) != 0);
        if (bias != 0) {
            min = Math.max(0, min + bias);
            max = Math.max(min, max + bias);
        }
        if (distance != 0) {
            min = Math.max(0, min + distance);
            max = Math.max(min, max + distance);
        }
        if (max <= min) {
            return Math.max(0, min);
        }

        StructureKey rollKey = key
                .or(() -> StructureLookup.enclosing(level, pos))
                .orElseGet(() -> regionKey(level, pos, matchedId, range));

        final int finalMin = min;
        final int finalMax = max;
        DifficultyPlus.debug(() -> String.format(
                "  key @ %d,%d,%d  source=%s  keyId=%s  keyCentre=%s  window=%d..%d",
                pos.getX(), pos.getY(), pos.getZ(),
                key.isPresent() ? "zone-structure"
                        : StructureLookup.enclosing(level, pos).isPresent() ? "enclosing-structure"
                        : range.biome_fill && BiomePatch.isResolved(BiomePatch.peek(level,
                                new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4)))
                                ? "biome-patch" : "region-grid",
                rollKey.structureId(), rollKey.centerPos().toShortString(),
                finalMin, finalMax));

        StructureContext context = new StructureContext(
                level, rollKey, rollKey.centerPos(), difficultyName,
                configuredLevel, min, max);

        int resolved = configuredLevel;
        for (LevelResolver resolver : DifficultyPlusInternals.levelResolvers()) {
            OptionalInt answer = resolver.resolve(context);
            if (answer.isPresent()) {
                resolved = answer.getAsInt();
                break;
            }
        }

        if (DifficultyPlusEvents.LEVEL_RESOLVED.isListened()) {
            DifficultyPlusEvents.MutableLevel mutable = new DifficultyPlusEvents.MutableLevel(resolved);
            DifficultyPlusEvents.LEVEL_RESOLVED.invoke(l -> l.onResolved(context, mutable));
            resolved = mutable.get();
        }
        return resolved;
    }

    private static ZoneData.LevelRange rangeFor(ServerLevel level, BlockPos pos, ResourceLocation matchedId) {
        Config config = ConfigBridge.config();
        if (config == null || config.dimensions == null || matchedId == null) {
            return null;
        }
        ResourceLocation dimensionId = level.dimension().location();
        for (Config.Dimension dimension : config.dimensions) {
            if (dimension.world_matches != null
                    && !matchesDimension(dimensionId, dimension.world_matches.dimension)) {
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
                if (zoneMentions(level, pos, zone, matchedId)) {
                    return data.level_range;
                }
            }
        }
        return null;
    }

    public static DistanceScaling scalingFor(ServerLevel level, BlockPos pos,
                                             ResourceLocation matchedId) {
        Config config = ConfigBridge.config();
        if (config == null || config.dimensions == null) {
            return null;
        }
        ResourceLocation dimensionId = level.dimension().location();
        for (Config.Dimension dimension : config.dimensions) {
            if (dimension.world_matches != null
                    && !matchesDimension(dimensionId, dimension.world_matches.dimension)) {
                continue;
            }
            if (dimension.zones != null && matchedId != null) {
                for (Config.Zone zone : dimension.zones) {
                    if (!zoneMentions(level, pos, zone, matchedId)) {
                        continue;
                    }
                    DistanceScaling zoneScaling = ConfigBridge.forZone(zone).distance_scaling;
                    if (zoneScaling != null && (zoneScaling.enabled || zoneScaling.override)) {
                        return zoneScaling.enabled ? zoneScaling : null;
                    }
                    break;
                }
            }
            DimensionData data = ConfigBridge.forDimension(dimension);
            return data.distance_scaling != null && data.distance_scaling.enabled
                    ? data.distance_scaling : null;
        }
        return null;
    }

    public static int distanceBonus(ServerLevel level, BlockPos pos, ResourceLocation matchedId) {
        DistanceScaling scaling = scalingFor(level, pos, matchedId);
        return scaling == null ? 0 : scaling.addedFor(level.getSharedSpawnPos(), pos);
    }

    public static boolean matchesDimension(ResourceLocation dimensionId, String pattern) {
        return pattern == null || pattern.isEmpty()
                || PatternMatching.universalMatch(dimensionId, pattern);
    }

    public static boolean zoneMentions(ServerLevel level,
                                BlockPos pos,
                                Config.Zone zone,
                                ResourceLocation matchedId) {
        if (zone.zone_matches == null) {
            return false;
        }
        return patternMatches(level, zone.zone_matches.structure, matchedId, Registries.STRUCTURE)
                || patternMatches(level, zone.zone_matches.biome, matchedId, Registries.BIOME)

                || biomeMatches(level, pos, zone.zone_matches.biome);
    }

    private static <T> boolean patternMatches(ServerLevel level,
                                              String pattern,
                                              ResourceLocation id,
                                              ResourceKey<Registry<T>> registryKey) {
        if (pattern == null || pattern.isEmpty() || id == null) {
            return false;
        }
        if (pattern.startsWith(NEGATE_PREFIX)) {
            String rest = pattern.substring(NEGATE_PREFIX.length());

            return !rest.isEmpty() && !patternMatches(level, rest, id, registryKey);
        }
        if (pattern.equals(ANY)) {
            return true;
        }
        if (pattern.startsWith(TAG_PREFIX)) {
            return inTag(level, id, registryKey, pattern.substring(TAG_PREFIX.length()));
        }
        return PatternMatching.idMatches(id, pattern);
    }

    private static <T> boolean inTag(ServerLevel level,
                                     ResourceLocation id,
                                     ResourceKey<Registry<T>> registryKey,
                                     String tagId) {
        if (tagId.isEmpty()) {
            return false;
        }
        ResourceLocation tagLocation = ResourceLocation.tryParse(tagId);
        if (tagLocation == null) {
            return false;
        }
        Registry<T> registry = level.registryAccess().registry(registryKey).orElse(null);
        if (registry == null) {
            return false;
        }
        return registry.getHolder(ResourceKey.create(registryKey, id))
                .map(holder -> holder.is(TagKey.create(registryKey, tagLocation)))
                .orElse(false);
    }

    private static boolean biomeMatches(ServerLevel level, BlockPos pos, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        Holder<Biome> biome = level.getBiome(pos);
        ResourceLocation biomeId = biome.unwrapKey().map(ResourceKey::location).orElse(null);
        return patternMatches(level, pattern, biomeId, Registries.BIOME);
    }

    private static StructureKey regionKey(ServerLevel level, BlockPos pos,
                                          ResourceLocation matchedId, ZoneData.LevelRange range) {
        ResourceLocation id = level.getBiome(pos).unwrapKey()
                .map(ResourceKey::location)
                .orElse(matchedId != null ? matchedId
                        : ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, "biome_zone"));

        if (range.biome_fill) {
            long patch = BiomePatch.idAt(level,
                    new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4),
                    range.biome_fill_cap,
                    ConfigBridge.global().biome_fill_budget);
            if (BiomePatch.isResolved(patch)) {
                ChunkPos anchor = new ChunkPos(patch);
                return StructureKey.of(id,
                        new BlockPos(anchor.getMiddleBlockX(), 0, anchor.getMiddleBlockZ()));
            }
        }

        int size = Math.max(1, range.region_size);
        int x = Math.floorDiv(pos.getX(), size) * size + size / 2;
        int z = Math.floorDiv(pos.getZ(), size) * size + size / 2;
        return StructureKey.of(id, new BlockPos(x, 0, z));
    }

    public static int hashedEntityLevel(long worldSeed, UUID id, int min, int max) {
        int span = max - min + 1;
        if (span <= 1) {
            return min;
        }
        long seed = worldSeed ^ id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 32);
        return min + (int) Math.floorMod(mix64(seed), span);
    }

    public static int hashedLevel(long worldSeed, StructureKey key, int min, int max) {
        int span = max - min + 1;
        if (span <= 1) {
            return min;
        }
        long seed = worldSeed ^ key.center() ^ ((long) key.structureId().hashCode() << 16);
        return min + (int) Math.floorMod(mix64(seed), span);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
