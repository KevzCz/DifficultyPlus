package net.pixeldreamstudios.difficultyplus.client;

import net.pixeldreamstudios.difficultyplus.client.zone.ZoneClientNetwork;
import net.pixeldreamstudios.difficultyplus.compat.XaeroCompat;

public final class DifficultyPlusClient {
    private DifficultyPlusClient() {}

    private static boolean initialised;

    public static void init() {
        if (initialised) {
            return;
        }
        initialised = true;
        ZoneClientNetwork.register();
        ZoneOverlayKey.register();
        XaeroCompat.init();
    }
}
