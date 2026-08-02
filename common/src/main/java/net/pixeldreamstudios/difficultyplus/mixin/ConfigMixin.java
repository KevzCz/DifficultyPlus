package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.config.Config;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Config.class)
public class ConfigMixin implements ConfigAccess.Global {
    public GlobalData difficultyplus = new GlobalData();

    @Override
    public GlobalData difficultyplus$global() {
        if (difficultyplus == null) {
            difficultyplus = new GlobalData();
        }
        return difficultyplus;
    }
}
