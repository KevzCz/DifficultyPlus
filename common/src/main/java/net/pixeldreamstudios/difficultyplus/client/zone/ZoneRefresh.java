package net.pixeldreamstudios.difficultyplus.client.zone;

import it.unimi.dsi.fastutil.longs.LongSet;

import java.util.ArrayList;
import java.util.List;

public final class ZoneRefresh {
    private ZoneRefresh() {}

    public interface Listener {
        void onRegionsChanged(LongSet regions, boolean full);
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();

    public static void addListener(Listener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    public static void requestFullRedraw() {
        if (LISTENERS.isEmpty()) {
            return;
        }
        ZoneChunkCache.drainDirty();
        for (Listener listener : LISTENERS) {
            listener.onRegionsChanged(LongSet.of(), true);
        }
    }

    public static void requestPending() {
        LongSet dirty = ZoneChunkCache.drainDirty();
        if (dirty.isEmpty() || LISTENERS.isEmpty()) {
            return;
        }
        boolean full = dirty.contains(Long.MIN_VALUE);
        for (Listener listener : LISTENERS) {
            listener.onRegionsChanged(dirty, full);
        }
    }
}
