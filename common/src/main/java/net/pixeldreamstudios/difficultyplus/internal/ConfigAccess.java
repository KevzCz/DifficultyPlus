package net.pixeldreamstudios.difficultyplus.internal;

import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.config.DimensionData;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;

public final class ConfigAccess {
    private ConfigAccess() {}

    public interface Global {
        GlobalData difficultyplus$global();
    }

    public interface Type {
        DifficultyPlusData difficultyplus$data();
        void difficultyplus$setData(DifficultyPlusData data);
    }

    public interface Zone {
        ZoneData difficultyplus$zone();
    }

    public interface Dimension {
        DimensionData difficultyplus$dimension();
    }

    public interface Entity {
        ZoneData difficultyplus$entity();
    }
}
