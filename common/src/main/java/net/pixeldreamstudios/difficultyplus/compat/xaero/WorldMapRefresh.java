package net.pixeldreamstudios.difficultyplus.compat.xaero;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapRegion;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

import java.util.List;

public final class WorldMapRefresh {
    private WorldMapRefresh() {}

    public static void refresh(LongSet regions, boolean full) {
        MapDimension dimension = dimension();
        if (dimension == null) {
            return;
        }
        DimensionHighlighterHandler handler = dimension.getHighlightHandler();
        if (handler == null) {
            return;
        }
        if (full) {
            handler.clearCachedHashes();
        } else {
            for (long key : regions) {
                if (key == Long.MIN_VALUE) {
                    continue;
                }
                ChunkPos region = new ChunkPos(key);
                handler.clearCachedHash(region.x, region.z);
            }
        }
        redraw(dimension, regions, full);
    }

    private static void redraw(MapDimension dimension, LongSet regions, boolean full) {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            return;
        }
        MapProcessor processor = session.getMapProcessor();
        if (processor == null) {
            return;
        }
        List<LeveledRegion<?>> loaded = dimension.getLayeredMapRegions().getLoadedListUnsynced();
        if (loaded == null) {
            return;
        }
        for (LeveledRegion<?> region : List.copyOf(loaded)) {
            if (!(region instanceof MapRegion leaf)) {
                continue;
            }
            if (!full && !regions.contains(ChunkPos.asLong(leaf.getRegionX(), leaf.getRegionZ()))) {
                continue;
            }
            leaf.updateTargetHighlightsHash();
            leaf.requestRefresh(processor);
        }
    }

    private static MapDimension dimension() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null) {
            return null;
        }
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null || session.getMapProcessor() == null) {
            return null;
        }
        MapWorld world = session.getMapProcessor().getMapWorld();
        if (world == null) {
            return null;
        }
        return world.getDimension(client.level.dimension());
    }
}
