package net.pixeldreamstudios.difficultyplus.client.zone;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.pixeldreamstudios.difficultyplus.network.ZoneInfo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ZoneChunkCache {
    private ZoneChunkCache() {}

    public record Entry(int zoneId, int level, byte[] quartZones, short[] quartLevels) {

        public Entry(int zoneId, int level) {
            this(zoneId, level, null, null);
        }

        public boolean hasDetail() {
            return quartZones != null && quartLevels != null;
        }

        public int zoneAt(int quart) {
            return hasDetail() ? quartZones[quart] & 0xFF : zoneId;
        }

        public int levelAt(int quart) {
            return hasDetail() ? quartLevels[quart] : level;
        }
    }

    private static final Map<ResourceLocation, Long2ObjectMap<Entry>> BY_DIMENSION =
            new HashMap<>();

    private static final Int2ObjectMap<ZoneInfo> ZONES = new Int2ObjectOpenHashMap<>();

    private static final LongSet DIRTY_REGIONS = new LongOpenHashSet();

    private static final Set<ResourceLocation> LOADED = new HashSet<>();

    private static int configHash;

    private static boolean hashKnown;

    private static String worldId;

    public static void setTable(int hash, List<ZoneInfo> zones) {
        boolean changed = hashKnown && hash != configHash;
        configHash = hash;
        hashKnown = true;
        ZONES.clear();
        for (ZoneInfo zone : zones) {
            ZONES.put(zone.id(), zone);
        }
        if (changed) {
            BY_DIMENSION.clear();
            LOADED.clear();
            ZoneStore.wipe(worldId);
        }
        markAllDirty();
    }

    public static void put(ResourceLocation dimension, long chunk, int zoneId, int level) {
        put(dimension, chunk, zoneId, level, null, null);
    }

    public static void put(ResourceLocation dimension, long chunk, int zoneId, int level,
                           byte[] quartZones, short[] quartLevels) {
        Long2ObjectMap<Entry> map = BY_DIMENSION.computeIfAbsent(dimension,
                key -> new Long2ObjectOpenHashMap<>());
        Entry existing = map.get(chunk);
        if (existing != null && existing.zoneId() == zoneId && existing.level() == level
                && Arrays.equals(existing.quartZones(), quartZones)
                && Arrays.equals(existing.quartLevels(), quartLevels)) {
            return;
        }
        map.put(chunk, new Entry(zoneId, level, quartZones, quartLevels));
        markDirty(chunk);
    }

    public static Entry get(ResourceLocation dimension, long chunk) {
        Long2ObjectMap<Entry> map = BY_DIMENSION.get(dimension);
        return map == null ? null : map.get(chunk);
    }

    public static ZoneInfo zone(int id) {
        return ZONES.get(id);
    }

    public static ZoneInfo zoneAt(ResourceLocation dimension, long chunk) {
        Entry entry = get(dimension, chunk);
        return entry == null ? null : ZONES.get(entry.zoneId());
    }

    public static boolean regionHasEntries(ResourceLocation dimension, int regionX, int regionZ) {
        Long2ObjectMap<Entry> map = BY_DIMENSION.get(dimension);
        if (map == null || map.isEmpty()) {
            return false;
        }
        int minX = regionX << 5;
        int minZ = regionZ << 5;
        for (int x = minX; x < minX + 32; x++) {
            for (int z = minZ; z < minZ + 32; z++) {
                if (map.containsKey(ChunkPos.asLong(x, z))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static int regionHash(ResourceLocation dimension, int regionX, int regionZ) {
        Long2ObjectMap<Entry> map = BY_DIMENSION.get(dimension);
        if (map == null || map.isEmpty()) {
            return 0;
        }
        int hash = 1;
        int minX = regionX << 5;
        int minZ = regionZ << 5;
        for (int x = minX; x < minX + 32; x++) {
            for (int z = minZ; z < minZ + 32; z++) {
                Entry entry = map.get(ChunkPos.asLong(x, z));
                if (entry != null) {
                    hash = hash * 31 + x;
                    hash = hash * 31 + z;
                    hash = hash * 31 + entry.zoneId();
                    hash = hash * 31 + entry.level();
                    hash = hash * 31 + Arrays.hashCode(entry.quartZones());
                    hash = hash * 31 + Arrays.hashCode(entry.quartLevels());
                }
            }
        }
        return hash;
    }

    public static Long2ObjectMap<Entry> raw(ResourceLocation dimension) {
        return BY_DIMENSION.get(dimension);
    }

    public static void load(String world, ResourceLocation dimension) {
        if (world == null || dimension == null || LOADED.contains(dimension)) {
            return;
        }
        worldId = world;
        LOADED.add(dimension);
        Long2ObjectMap<Entry> loaded = ZoneStore.read(world, dimension, configHash);
        if (loaded != null && !loaded.isEmpty()) {
            BY_DIMENSION.merge(dimension, loaded, (existing, stored) -> {
                stored.putAll(existing);
                return stored;
            });
            markAllDirty();
        }
    }

    public static void loadCurrent() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || worldId == null) {
            return;
        }
        load(worldId, client.level.dimension().location());
    }

    public static void save() {
        if (worldId == null) {
            return;
        }
        for (Map.Entry<ResourceLocation, Long2ObjectMap<Entry>> entry : BY_DIMENSION.entrySet()) {
            ZoneStore.write(worldId, entry.getKey(), configHash, entry.getValue());
        }
    }

    public static void clear() {
        BY_DIMENSION.clear();
        ZONES.clear();
        DIRTY_REGIONS.clear();
        LOADED.clear();
        configHash = 0;
        hashKnown = false;
        worldId = null;
    }

    public static void setWorldId(String id) {
        worldId = id;
    }

    private static void markDirty(long chunk) {
        ChunkPos pos = new ChunkPos(chunk);
        DIRTY_REGIONS.add(ChunkPos.asLong(pos.x >> 5, pos.z >> 5));
    }

    public static void markAllDirty() {
        DIRTY_REGIONS.add(Long.MIN_VALUE);
    }

    public static LongSet drainDirty() {
        if (DIRTY_REGIONS.isEmpty()) {
            return LongSet.of();
        }
        LongSet copy = new LongOpenHashSet(DIRTY_REGIONS);
        DIRTY_REGIONS.clear();
        return copy;
    }

    public static boolean isFullRefresh(long key) {
        return key == Long.MIN_VALUE;
    }
}
