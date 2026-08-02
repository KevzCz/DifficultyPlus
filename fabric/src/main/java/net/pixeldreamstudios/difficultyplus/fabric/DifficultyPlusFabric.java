package net.pixeldreamstudios.difficultyplus.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

public final class DifficultyPlusFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        DifficultyPlus.init();

        ServerLifecycleEvents.SERVER_STARTED.register(DifficultyPlus::onServerStarted);
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> DifficultyPlus.registerCommands(dispatcher));
    }
}
