package net.pixeldreamstudios.difficultyplus.client;

import dev.architectury.event.events.client.ClientTickEvent;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
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
        ClientTickEvent.CLIENT_POST.register(client -> ZoneChunkCache.tick());
    }
}
