package net.pixeldreamstudios.difficultyplus;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.internal.command.LevelCommands;
import net.pixeldreamstudios.difficultyplus.internal.ConfigValidator;
import net.pixeldreamstudios.difficultyplus.internal.damage.ConfigResistanceProvider;
import net.pixeldreamstudios.difficultyplus.internal.effect.BuiltInEffects;
import net.pixeldreamstudios.difficultyplus.internal.level.BuiltInResolvers;
import net.pixeldreamstudios.difficultyplus.internal.structure.OwnershipStamp;
import net.pixeldreamstudios.difficultyplus.internal.structure.PlacementTracker;
import net.pixeldreamstudios.difficultyplus.internal.structure.RefreshQueue;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneChunkSync;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneTable;
import net.pixeldreamstudios.difficultyplus.network.DPNetwork;
import net.pixeldreamstudios.difficultyplus.item.DPItems;
import net.pixeldreamstudios.difficultyplus.item.bias.BiasReloadListener;
import net.pixeldreamstudios.difficultyplus.internal.ReloadHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.Set;
import java.util.UUID;

public final class DifficultyPlus {
    public static final String MOD_ID = "difficultyplus";
    public static final Logger LOGGER = LoggerFactory.getLogger("Difficulty+");

    private static volatile boolean debugEnabled = Boolean.getBoolean("difficultyplus.debug");

    private static final Set<UUID> debugListeners =
            ConcurrentHashMap.newKeySet();

    private static MinecraftServer server;

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    public static void setDebug(UUID player, boolean enabled) {
        debugEnabled = enabled;
        resetDebugSeen();
        if (enabled) {
            debugListeners.add(player);
        } else {
            debugListeners.remove(player);
        }
    }

    private static final Set<String> debugSeen =
            ConcurrentHashMap.newKeySet();

    public static void debugOnce(String key, Supplier<String> message) {
        if (debugEnabled && debugSeen.add(key)) {
            debug(message);
        }
    }

    public static void resetDebugSeen() {
        debugSeen.clear();
    }

    public static void debug(Supplier<String> message) {
        if (!debugEnabled) {
            return;
        }
        String text = message.get();
        LOGGER.info("[dp] {}", text);

        MinecraftServer current = server;
        if (current == null || debugListeners.isEmpty()) {
            return;
        }

        current.execute(() -> {
            for (UUID id : debugListeners) {
                ServerPlayer player = current.getPlayerList().getPlayer(id);
                if (player != null) {
                    player.sendSystemMessage(Component
                            .literal("[dp] " + text)
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }
        });
    }

    private DifficultyPlus() {}

    public static void init() {
        BuiltInEffects.register();
        BuiltInResolvers.register();
        DifficultyPlusAPI.registerResistanceProvider(new ConfigResistanceProvider());
        PlacementTracker.register();
        OwnershipStamp.register();

        LifecycleEvent.SERVER_STOPPING.register(server -> RefreshQueue.abandonAll());
        LifecycleEvent.SERVER_LEVEL_UNLOAD.register(RefreshQueue::abandon);

        DPNetwork.registerServer();
        PlayerEvent.PLAYER_JOIN.register(ZoneChunkSync::onJoin);
        PlayerEvent.PLAYER_QUIT.register(ZoneChunkSync::onLeave);
        DPItems.register();
        ReloadListenerRegistry.register(PackType.SERVER_DATA, new BiasReloadListener(),
                ResourceLocation.fromNamespaceAndPath(MOD_ID, "biases"));
    }

    public static void onServerStarted(MinecraftServer startedServer) {
        server = startedServer;
        ReloadHandler.setServer(startedServer);
        ConfigValidator.validate();
        ZoneTable.rebuild();
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LevelCommands.register(dispatcher);
    }
}
