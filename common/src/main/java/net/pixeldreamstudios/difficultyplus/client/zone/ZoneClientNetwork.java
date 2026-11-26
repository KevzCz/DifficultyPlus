package net.pixeldreamstudios.difficultyplus.client.zone;

import net.pixeldreamstudios.difficultyplus.network.PlayerLevelPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.level.storage.LevelResource;
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
                    ZoneRefresh.requestFullRedraw();
                }));

        NetworkManager.registerReceiver(NetworkManager.Side.S2C,
                PlayerLevelPayload.TYPE, PlayerLevelPayload.STREAM_CODEC,
                (payload, context) -> context.queue(() -> ZoneStyles.playerLevel(payload.level())));

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
                    if (ZoneChunkCache.consumeTableReset()) {
                        ZoneRefresh.requestFullRedraw();
                    } else {
                        ZoneRefresh.requestPending();
                    }
                }));
    }

    public static String worldId() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            return "sp_" + client.getSingleplayerServer().getWorldPath(LevelResource.ROOT).normalize().getFileName();
        }
        ServerData data = client.getCurrentServer();
        return data == null ? null : "mp_" + data.ip;
    }
}
