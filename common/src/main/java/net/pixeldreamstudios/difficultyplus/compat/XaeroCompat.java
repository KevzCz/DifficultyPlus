package net.pixeldreamstudios.difficultyplus.compat;

import dev.architectury.platform.Platform;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.compat.xaero.XaeroBridge;

public final class XaeroCompat {
    private XaeroCompat() {}

    private static boolean minimap;

    private static boolean worldmap;

    public static void init() {
        minimap = Platform.isModLoaded("xaerominimap");
        worldmap = Platform.isModLoaded("xaeroworldmap");

        if (!minimap && !worldmap) {
            return;
        }
        try {
            XaeroBridge.install();
            DifficultyPlus.LOGGER.info("Difficulty+ zone overlay active (minimap={}, worldmap={})",
                    minimap, worldmap);
        } catch (Throwable t) {
            minimap = false;
            worldmap = false;
            DifficultyPlus.LOGGER.warn("Difficulty+ could not attach to Xaero's maps", t);
        }
    }

    public static boolean minimapLoaded() {
        return minimap;
    }

    public static boolean worldmapLoaded() {
        return worldmap;
    }
}
