package net.pixeldreamstudios.difficultyplus.internal.zone;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.network.DPNetwork;
import net.pixeldreamstudios.difficultyplus.network.ZoneChunkPayload;
import net.pixeldreamstudios.difficultyplus.network.ZoneTablePayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ZoneChunkSync {
    private ZoneChunkSync() {}

    private static final Map<UUID, Long2IntMap> SENT = new HashMap<>();

    private static final Map<UUID, List<Pending>> QUEUE = new HashMap<>();

    private record Pending(long chunk, int zoneId, int level,
                           byte[] quartZones, short[] quartLevels) {}

    public static void onJoin(ServerPlayer player) {
        if (!enabled()) {
            return;
        }
        SENT.remove(player.getUUID());
        DPNetwork.send(player, new ZoneTablePayload(ZoneTable.configHash(), ZoneTable.zones()));
    }

    public static void onLeave(ServerPlayer player) {
        SENT.remove(player.getUUID());
        QUEUE.remove(player.getUUID());
    }

    public static void onConfigReload(Iterable<ServerPlayer> players) {
        ZoneTable.rebuild();
        SENT.clear();
        QUEUE.clear();
        if (!enabled()) {
            return;
        }
        for (ServerPlayer player : players) {
            DPNetwork.send(player, new ZoneTablePayload(ZoneTable.configHash(), ZoneTable.zones()));
            resendLoaded(player);
        }
    }

    private static void resendLoaded(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        int radius = level.getServer().getPlayerList().getViewDistance();
        ChunkPos centre = player.chunkPosition();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (level.getChunkSource().getChunkNow(centre.x + dx, centre.z + dz) != null) {
                    onChunkSent(player, new ChunkPos(centre.x + dx, centre.z + dz));
                }
            }
        }
    }

    public static void onChunkSent(ServerPlayer player, ChunkPos pos) {
        if (!enabled() || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        int cap = ConfigBridge.global().zone_map.send_distance;
        if (cap > 0) {
            int dx = pos.x - player.chunkPosition().x;
            int dz = pos.z - player.chunkPosition().z;
            if (dx * dx + dz * dz > cap * cap) {
                return;
            }
        }

        ChunkAccess chunk = level.getChunkSource().getChunkNow(pos.x, pos.z);
        if (chunk == null) {
            return;
        }
        int centreX = pos.getMiddleBlockX();
        int centreZ = pos.getMiddleBlockZ();
        int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, centreX & 15, centreZ & 15) - 1;
        BlockPos centre = new BlockPos(centreX, Mth.clamp(surface,
                level.getMinBuildHeight(), level.getMaxBuildHeight() - 1), centreZ);
        Optional<ZoneContext> zone = ZoneResolver.at(level, centre);
        int zoneId = zone.map(context -> ZoneTable.idOf(context.difficulty())).orElse(0);
        int levelValue = zone.map(ZoneContext::zoneLevel).orElse(0);
        if (zoneId == 0) {
            return;
        }

        byte[] quartZones = null;
        short[] quartLevels = null;
        int detailHash = 0;
        if (ConfigBridge.global().zone_map.quart_detail) {
            byte[] zones = new byte[ZoneChunkPayload.QUARTS];
            short[] levels = new short[ZoneChunkPayload.QUARTS];
            boolean uniform = true;
            for (int q = 0; q < ZoneChunkPayload.QUARTS; q++) {
                int qx = pos.getMinBlockX() + (q % ZoneChunkPayload.QUART_SPAN) * 4 + 2;
                int qz = pos.getMinBlockZ() + (q / ZoneChunkPayload.QUART_SPAN) * 4 + 2;
                int qy = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, qx & 15, qz & 15) - 1;
                Optional<ZoneContext> at = ZoneResolver.at(level, new BlockPos(qx,
                        Mth.clamp(qy, level.getMinBuildHeight(), level.getMaxBuildHeight() - 1), qz));
                zones[q] = (byte) at.map(c -> ZoneTable.idOf(c.difficulty())).orElse(0).intValue();
                levels[q] = (short) at.map(ZoneContext::zoneLevel).orElse(0).intValue();
                if (zones[q] != (byte) zoneId || levels[q] != (short) levelValue) {
                    uniform = false;
                }
                detailHash = detailHash * 31 + zones[q] * 31 + levels[q];
            }
            if (!uniform) {
                quartZones = zones;
                quartLevels = levels;
            }
        }

        long key = pos.toLong();
        int packed = (zoneId << 16) | (levelValue & 0xFFFF) ^ detailHash;
        Long2IntMap sent = SENT.computeIfAbsent(player.getUUID(), id -> new Long2IntOpenHashMap());
        if (sent.get(key) == packed) {
            return;
        }
        sent.put(key, packed);
        QUEUE.computeIfAbsent(player.getUUID(), id -> new ArrayList<>())
                .add(new Pending(key, zoneId, levelValue, quartZones, quartLevels));
    }

    public static void flush(ServerLevel level) {
        if (QUEUE.isEmpty()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            List<Pending> pending = QUEUE.remove(player.getUUID());
            if (pending == null || pending.isEmpty()) {
                continue;
            }
            LongList chunks = new LongArrayList(pending.size());
            byte[] zoneIds = new byte[pending.size()];
            short[] levels = new short[pending.size()];
            byte[][] quartZones = new byte[pending.size()][];
            short[][] quartLevels = new short[pending.size()][];
            for (int i = 0; i < pending.size(); i++) {
                Pending entry = pending.get(i);
                chunks.add(entry.chunk());
                zoneIds[i] = (byte) entry.zoneId();
                levels[i] = (short) entry.level();
                quartZones[i] = entry.quartZones();
                quartLevels[i] = entry.quartLevels();
            }
            DPNetwork.send(player, new ZoneChunkPayload(
                    level.dimension().location(), chunks, zoneIds, levels,
                    quartZones, quartLevels));
        }
    }

    private static boolean enabled() {
        GlobalData.ZoneMap config = ConfigBridge.global().zone_map;
        return config != null && config.enabled;
    }
}
