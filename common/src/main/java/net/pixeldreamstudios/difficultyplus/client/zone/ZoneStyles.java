package net.pixeldreamstudios.difficultyplus.client.zone;

import net.pixeldreamstudios.difficultyplus.config.ClientData;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;

import java.util.Objects;
import java.util.OptionalInt;

public final class ZoneStyles {

    public enum Map { MINIMAP, WORLD_MAP }

    private static final int UNKNOWN_LEVEL = -1;

    private static boolean peeking;
    private static int playerLevel = UNKNOWN_LEVEL;

    private ZoneStyles() {
    }

    public static boolean drawn(Map map) {
        if (peeking) {
            return true;
        }
        ClientData config = ClientConfig.get();
        return config.show_overlay && (map == Map.MINIMAP ? config.minimap_overlay : config.world_map_overlay);
    }

    public static boolean tooltips() {
        return ClientConfig.get().show_tooltips;
    }

    public static boolean overlayShown() {
        return ClientConfig.get().show_overlay;
    }

    public static boolean toggleOverlay() {
        ClientData config = ClientConfig.get();
        config.show_overlay = !config.show_overlay;
        ClientConfig.save();
        redraw();
        return config.show_overlay;
    }

    public static boolean active(Map map) {
        return drawn(map) || tooltips();
    }

    public static ClientData.OverlayStyle style(Map map) {
        if (peeking) {
            return ClientData.OverlayStyle.FULL;
        }
        ClientData config = ClientConfig.get();
        return map == Map.MINIMAP ? config.minimapStyle() : config.worldMapStyle();
    }

    public static float strength() {
        return peeking ? 1f : Math.max(0f, Math.min(1f, ClientConfig.get().overlay_opacity));
    }

    public static ClientData.ColourMode mode() {
        ClientData.ColourMode mode = ClientConfig.get().colourMode();
        return mode == ClientData.ColourMode.RELATIVE && playerLevel == UNKNOWN_LEVEL ? ClientData.ColourMode.LEVEL : mode;
    }

    public static OptionalInt playerLevel() {
        return playerLevel == UNKNOWN_LEVEL ? OptionalInt.empty() : OptionalInt.of(playerLevel);
    }

    public static void peek(boolean on) {
        if (peeking != on) {
            peeking = on;
            redraw();
        }
    }

    public static void playerLevel(int level) {
        if (playerLevel == level) {
            return;
        }
        playerLevel = level;
        if (ClientConfig.get().colourMode() == ClientData.ColourMode.RELATIVE) {
            redraw();
        }
    }

    public static int hash(Map map) {
        return Objects.hash(drawn(map), style(map), mode(), mode() == ClientData.ColourMode.RELATIVE ? playerLevel : UNKNOWN_LEVEL, ClientConfig.get().smoothing,
                strength());
    }

    public static void redraw() {
        ZoneChunkCache.markAllDirty();
        ZoneRefresh.requestFullRedraw();
    }
}
