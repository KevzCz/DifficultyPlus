package net.pixeldreamstudios.difficultyplus.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.level.ServerPlayer;

public final class DPNetwork {
    private DPNetwork() {}

    public static void registerServer() {
        if (Platform.getEnvironment() == Env.CLIENT) {
            return;
        }
        NetworkManager.registerS2CPayloadType(ZoneTablePayload.TYPE, ZoneTablePayload.STREAM_CODEC);
        NetworkManager.registerS2CPayloadType(ZoneChunkPayload.TYPE, ZoneChunkPayload.STREAM_CODEC);
    }

    public static void send(ServerPlayer player, ZoneTablePayload payload) {
        if (NetworkManager.canPlayerReceive(player, ZoneTablePayload.TYPE)) {
            NetworkManager.sendToPlayer(player, payload);
        }
    }

    public static void send(ServerPlayer player, ZoneChunkPayload payload) {
        if (NetworkManager.canPlayerReceive(player, ZoneChunkPayload.TYPE)) {
            NetworkManager.sendToPlayer(player, payload);
        }
    }
}
