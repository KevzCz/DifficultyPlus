package net.pixeldreamstudios.difficultyplus.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.loader.api.FabricLoader;
import net.pixeldreamstudios.difficultyplus.client.config.DifficultyPlusConfigScreen;

public class DifficultyPlusModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> FabricLoader.getInstance().isModLoaded("cloth-config")
                ? DifficultyPlusConfigScreen.create(parent)
                : parent;
    }
}
