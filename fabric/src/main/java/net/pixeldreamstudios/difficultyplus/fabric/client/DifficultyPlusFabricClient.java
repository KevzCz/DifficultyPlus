package net.pixeldreamstudios.difficultyplus.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.pixeldreamstudios.difficultyplus.client.DifficultyPlusClient;

public final class DifficultyPlusFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        DifficultyPlusClient.init();
    }
}
