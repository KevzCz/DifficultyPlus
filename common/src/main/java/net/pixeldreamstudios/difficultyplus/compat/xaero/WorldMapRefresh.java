package net.pixeldreamstudios.difficultyplus.compat.xaero;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.ChunkPos;
import xaero.map.highlight.DimensionHighlighterHandler;

import java.util.ArrayList;
import java.util.List;

public final class WorldMapRefresh {
    private WorldMapRefresh() {}

    private static final List<DimensionHighlighterHandler> HANDLERS = new ArrayList<>();

    public static void track(DimensionHighlighterHandler handler) {
        if (handler != null && !HANDLERS.contains(handler)) {
            HANDLERS.add(handler);
        }
    }

    public static void refresh(LongSet regions, boolean full) {
        if (HANDLERS.isEmpty()) {
            return;
        }
        for (DimensionHighlighterHandler handler : HANDLERS) {
            if (full) {
                handler.clearCachedHashes();
                continue;
            }
            for (long key : regions) {
                if (key == Long.MIN_VALUE) {
                    continue;
                }
                ChunkPos region = new ChunkPos(key);
                handler.clearCachedHash(region.x, region.z);
            }
        }
    }
}
