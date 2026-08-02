package net.pixeldreamstudios.difficultyplus.compat.xaero;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.level.ChunkPos;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneRefresh;
import net.pixeldreamstudios.difficultyplus.compat.XaeroCompat;

public final class XaeroBridge {
    private XaeroBridge() {}

    private static MinimapZoneHighlighter minimapHighlighter;

    private static WorldMapZoneHighlighter worldMapHighlighter;

    public static void install() {
        ZoneRefresh.addListener(XaeroBridge::onRegionsChanged);
    }

    public static MinimapZoneHighlighter minimapHighlighter() {
        if (minimapHighlighter == null) {
            minimapHighlighter = new MinimapZoneHighlighter();
        }
        return minimapHighlighter;
    }

    public static WorldMapZoneHighlighter worldMapHighlighter() {
        if (worldMapHighlighter == null) {
            worldMapHighlighter = new WorldMapZoneHighlighter();
        }
        return worldMapHighlighter;
    }

    private static void onRegionsChanged(LongSet regions, boolean full) {
        try {
            if (XaeroCompat.minimapLoaded()) {
                MinimapRefresh.refresh(regions, full);
            }
            if (XaeroCompat.worldmapLoaded()) {
                WorldMapRefresh.refresh(regions, full);
            }
        } catch (Throwable t) {
            DifficultyPlus.LOGGER.debug("Difficulty+ zone refresh failed", t);
        }
    }

    static ChunkPos regionToChunk(long key) {
        return new ChunkPos(key);
    }
}
