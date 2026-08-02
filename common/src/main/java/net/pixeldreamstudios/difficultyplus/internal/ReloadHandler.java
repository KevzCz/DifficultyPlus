package net.pixeldreamstudios.difficultyplus.internal;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.internal.effect.ZoneEffectManager;
import net.pixeldreamstudios.difficultyplus.internal.zone.BiomePatch;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneChunkSync;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneResolver;

import java.util.ArrayList;
import java.util.List;

public final class ReloadHandler {
    private ReloadHandler() {}

    private static MinecraftServer server;

    public static void setServer(MinecraftServer instance) {
        server = instance;
    }

    public static MinecraftServer server() {
        return server;
    }

    public static void afterConfigReload() {
        try {
            ZoneResolver.invalidate();
            BiomePatch.invalidate();
            ConfigValidator.validate();

            List<ServerLevel> levels = new ArrayList<>();
            if (server != null) {
                server.getAllLevels().forEach(levels::add);
            }
            ZoneEffectManager.reload(levels);

            if (server != null) {
                ZoneChunkSync.onConfigReload(server.getPlayerList().getPlayers());
            }
        } catch (Exception e) {
            DifficultyPlus.LOGGER.error("Failed to rebuild Difficulty+ state after config reload", e);
        }
    }
}
