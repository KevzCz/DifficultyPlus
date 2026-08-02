package net.pixeldreamstudios.difficultyplus.internal.config;

import net.dungeon_difficulty.DungeonDifficulty;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

public final class ConfigSaver {
    private ConfigSaver() {}

    public static boolean saveAndReload() {
        try {
            DungeonDifficulty.config.save();
            DungeonDifficulty.reloadConfig();
            return true;
        } catch (Throwable t) {

            DifficultyPlus.LOGGER.error("Could not save Dungeon Difficulty config", t);
            return false;
        }
    }
}
