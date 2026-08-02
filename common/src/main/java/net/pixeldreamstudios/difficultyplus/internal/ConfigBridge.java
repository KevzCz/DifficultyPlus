package net.pixeldreamstudios.difficultyplus.internal;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.DifficultyTypes;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.config.DimensionData;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;

public final class ConfigBridge {
    private ConfigBridge() {}

    private static final GlobalData FALLBACK_GLOBAL = new GlobalData();
    private static final DifficultyPlusData EMPTY_DATA = new DifficultyPlusData();

    private static volatile Config current;

    public static void capture(Config config) {
        current = config;
    }

    public static Config config() {
        return current;
    }

    public static GlobalData global() {
        Config config = current;
        if (config == null) {
            return FALLBACK_GLOBAL;
        }
        return ((ConfigAccess.Global) (Object) config).difficultyplus$global();
    }

    public static DifficultyPlusData forDifficulty(String name) {
        if (name == null) {
            return EMPTY_DATA;
        }
        for (Config.DifficultyType type : DifficultyTypes.resolved) {
            if (name.equals(type.name)) {
                return ((ConfigAccess.Type) (Object) type).difficultyplus$data();
            }
        }
        return EMPTY_DATA;
    }

    public static ZoneData forZone(Config.Zone zone) {
        return ((ConfigAccess.Zone) (Object) zone).difficultyplus$zone();
    }

    public static DimensionData forDimension(Config.Dimension dimension) {
        return ((ConfigAccess.Dimension) (Object) dimension).difficultyplus$dimension();
    }
}
