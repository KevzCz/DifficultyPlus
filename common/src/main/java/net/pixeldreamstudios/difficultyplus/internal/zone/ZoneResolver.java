package net.pixeldreamstudios.difficultyplus.internal.zone;

import net.dungeon_difficulty.logic.PatternMatching;
import net.dungeon_difficulty.logic.ScalingGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ZoneResolver {
    private ZoneResolver() {}

    private static final Map<ResourceLocation, Map<CacheKey, Entry>> CACHE = new HashMap<>();

    private record CacheKey(long quart, int section, ResourceLocation biome) {}

    private static final long TTL_TICKS = 20L;

    private record Entry(Optional<ZoneContext> value, long tick) {}

    public static Optional<ZoneContext> at(ServerLevel level, BlockPos pos) {
        ResourceLocation dimension = level.dimension().location();
        CacheKey key = new CacheKey(
                ChunkPos.asLong(pos.getX() >> 2, pos.getZ() >> 2),
                pos.getY() >> 4,
                level.getBiome(pos).unwrapKey().map(ResourceKey::location).orElse(null));

        long now = level.getGameTime();
        Map<CacheKey, Entry> byKey = CACHE.computeIfAbsent(dimension, k -> new HashMap<>());
        Entry cached = byKey.get(key);
        if (cached != null && now - cached.tick() < TTL_TICKS) {
            return cached.value();
        }

        Optional<ZoneContext> resolved = compute(level, pos);
        byKey.put(key, new Entry(resolved, now));
        return resolved;
    }

    private static Optional<ZoneContext> compute(ServerLevel level, BlockPos pos) {
        try {
            PatternMatching.LocationData location = PatternMatching.LocationData.create(level, pos);
            PatternMatching.DifficultySearchResult result =
                    PatternMatching.getDifficultyResult(location, null, ScalingGoal.ENTITY, level);
            if (result == null || result.difficulty() == null || !result.difficulty().isValid()) {
                return Optional.empty();
            }

            String name = result.difficulty().type().name;
            ResourceLocation matched = result.matchId();

            int resolvedLevel = result.difficulty().level();

            return Optional.of(new ZoneContext(
                    level,
                    name,
                    resolvedLevel,
                    Optional.ofNullable(matched),
                    ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, "zone/" + name)));
        } catch (Exception e) {
            DifficultyPlus.LOGGER.debug("Zone lookup failed at {}", pos, e);
            return Optional.empty();
        }
    }

    public static void invalidate() {
        CACHE.clear();
    }
}
