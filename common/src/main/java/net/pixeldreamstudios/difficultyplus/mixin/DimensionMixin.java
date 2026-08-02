package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.config.Config;
import net.pixeldreamstudios.difficultyplus.config.DimensionData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Config.Dimension.class)
public class DimensionMixin implements ConfigAccess.Dimension {

    public DimensionData difficultyplus = new DimensionData();

    @Override
    public DimensionData difficultyplus$dimension() {
        if (difficultyplus == null) {
            difficultyplus = new DimensionData();
        }
        return difficultyplus;
    }
}
