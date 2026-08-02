package net.pixeldreamstudios.difficultyplus.internal.zone;

import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

import java.util.HashMap;
import java.util.Map;

public final class BiomePatch {
    private BiomePatch() {}

    private static final long UNRESOLVED = Long.MIN_VALUE;

    private static final Map<ResourceLocation, Long2LongMap> BY_DIMENSION = new HashMap<>();

    private static int fillsThisTick;

    public static void resetTickBudget() {
        fillsThisTick = 0;
    }

    public static long idAt(ServerLevel level, ChunkPos origin, int cap, int budgetPerTick) {
        ResourceLocation dimension = level.dimension().location();
        Long2LongMap cache = BY_DIMENSION.computeIfAbsent(dimension, key -> {
            Long2LongOpenHashMap created = new Long2LongOpenHashMap();
            created.defaultReturnValue(UNRESOLVED);
            return created;
        });

        long known = cache.get(origin.toLong());
        if (known != UNRESOLVED) {
            return known;
        }
        if (budgetPerTick > 0 && fillsThisTick >= budgetPerTick) {
            return UNRESOLVED;
        }
        fillsThisTick++;
        return fill(level, origin, cap, cache);
    }

    private static long fill(ServerLevel level, ChunkPos origin, int cap, Long2LongMap cache) {
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState().sampler();

        ResourceLocation target = biomeAt(source, sampler, origin);
        if (target == null) {
            return UNRESOLVED;
        }

        int limit = Math.max(1, cap);
        LongSet seen = new LongOpenHashSet();
        LongSet matched = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(origin.toLong());
        seen.add(origin.toLong());
        matched.add(origin.toLong());

        long lowest = origin.toLong();
        int lowestX = origin.x;
        int lowestZ = origin.z;
        boolean capped = false;

        while (!queue.isEmpty()) {
            if (matched.size() > limit) {
                capped = true;
                break;
            }
            long current = queue.dequeueLong();
            ChunkPos pos = new ChunkPos(current);

            if (pos.z < lowestZ || (pos.z == lowestZ && pos.x < lowestX)) {
                lowestX = pos.x;
                lowestZ = pos.z;
                lowest = current;
            }

            for (int side = 0; side < 4; side++) {
                int nx = pos.x + (side == 0 ? 1 : side == 1 ? -1 : 0);
                int nz = pos.z + (side == 2 ? 1 : side == 3 ? -1 : 0);
                long neighbour = ChunkPos.asLong(nx, nz);
                if (!seen.add(neighbour)) {
                    continue;
                }
                if (target.equals(biomeAt(source, sampler, new ChunkPos(nx, nz)))) {
                    matched.add(neighbour);
                    queue.enqueue(neighbour);
                }
            }
        }

        if (capped) {
            DifficultyPlus.debug(() -> String.format(
                    "biome fill capped at %d chunks from %s biome=%s",
                    limit, origin, target));
            return UNRESOLVED;
        }

        long id = lowest;
        for (long chunk : matched) {
            cache.put(chunk, id);
        }

        final int size = matched.size();
        DifficultyPlus.debug(() -> String.format(
                "biome fill %s biome=%s chunks=%d id=%s",
                origin, target, size, new ChunkPos(id)));
        return id;
    }

    private static ResourceLocation biomeAt(BiomeSource source, Climate.Sampler sampler,
                                            ChunkPos pos) {
        Holder<Biome> holder = source.getNoiseBiome(
                QuartPos.fromBlock(pos.getMiddleBlockX()),
                QuartPos.fromBlock(64),
                QuartPos.fromBlock(pos.getMiddleBlockZ()),
                sampler);
        return holder.unwrapKey().map(ResourceKey::location).orElse(null);
    }

    public static long peek(ServerLevel level, ChunkPos pos) {
        Long2LongMap cache = BY_DIMENSION.get(level.dimension().location());
        return cache == null ? UNRESOLVED : cache.get(pos.toLong());
    }

    public static void invalidate() {
        BY_DIMENSION.clear();
    }

    public static boolean isResolved(long id) {
        return id != UNRESOLVED;
    }
}
