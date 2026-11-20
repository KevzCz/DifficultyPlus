package net.pixeldreamstudios.difficultyplus.internal.level;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.network.DPNetwork;
import net.pixeldreamstudios.difficultyplus.network.PlayerLevelPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerLevelSync {
    private PlayerLevelSync() {}

    private static final Map<UUID, Integer> SENT = new HashMap<>();

    public static void tick(ServerLevel level) {
        int interval = ConfigBridge.global().pollInterval();
        for (ServerPlayer player : level.players()) {
            if ((player.tickCount + player.getId()) % interval != 0) {
                continue;
            }
            int current = LevelGap.playerLevel(player).orElse(PlayerLevelPayload.UNKNOWN);
            Integer last = SENT.put(player.getUUID(), current);
            if (last == null || last != current) {
                DPNetwork.send(player, new PlayerLevelPayload(current));
            }
        }
    }

    public static void forget(UUID player) {
        SENT.remove(player);
    }
}
