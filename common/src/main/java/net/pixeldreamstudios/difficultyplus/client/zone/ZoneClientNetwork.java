package net.pixeldreamstudios.difficultyplus.client.zone;

import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.pixeldreamstudios.difficultyplus.network.ZoneChunkPayload;
import net.pixeldreamstudios.difficultyplus.network.ZoneTablePayload;

public final class ZoneClientNetwork {
    private ZoneClientNetwork() {}

    public static void register() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                ZoneTablePayload.TYPE, ZoneTablePayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    ZoneChunkCache.setWorldId(worldId());
                    ZoneChunkCache.setTable(payload.configHash(), payload.zones());
                    ZoneChunkCache.loadCurrent();
                }));

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                ZoneChunkPayload.TYPE, ZoneChunkPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> {
                    for (int i = 0; i < payload.chunks().size(); i++) {
                        ZoneChunkCache.put(payload.dimension(),
                                payload.chunks().getLong(i),
                                payload.zoneIds()[i] & 0xFF,
                                payload.levels()[i],
                                payload.quartZones()[i],
                                payload.quartLevels()[i]);
                    }
                    ZoneRefresh.requestPending();
                }));
    }

    public static String worldId() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            return "sp_" + client.getSingleplayerServer().getWorldData().getLevelName();
        }
        ServerData data = client.getCurrentServer();
        return data == null ? null : "mp_" + data.ip;
    }
}
