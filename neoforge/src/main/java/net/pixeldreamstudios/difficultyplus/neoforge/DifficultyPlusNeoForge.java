package net.pixeldreamstudios.difficultyplus.neoforge;

import net.pixeldreamstudios.difficultyplus.client.DifficultyPlusClient;
import net.pixeldreamstudios.difficultyplus.client.config.DifficultyPlusConfigScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

@Mod(DifficultyPlus.MOD_ID)
public final class DifficultyPlusNeoForge {

    public DifficultyPlusNeoForge(IEventBus modBus, ModContainer container) {
        DifficultyPlus.init();

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class,
                event -> DifficultyPlus.registerCommands(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class,
                event -> DifficultyPlus.onServerStarted(event.getServer()));

        if (FMLEnvironment.dist == Dist.CLIENT) {
            DifficultyPlusClient.init();
            NeoForgeConfigScreen.register(container);
        }
    }

    private static final class NeoForgeConfigScreen {
        private NeoForgeConfigScreen() {}

        static void register(ModContainer container) {
            if (!ModList.get().isLoaded("cloth_config")) {
                return;
            }
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (ignored, parent) -> DifficultyPlusConfigScreen.create(parent));
        }
    }
}
