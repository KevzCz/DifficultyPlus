package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.config.Config;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Config.DifficultyType.class)
public class DifficultyTypeMixin implements ConfigAccess.Type {

    public DifficultyPlusData difficultyplus = new DifficultyPlusData();

    @Override
    public DifficultyPlusData difficultyplus$data() {
        if (difficultyplus == null) {
            difficultyplus = new DifficultyPlusData();
        }
        return difficultyplus;
    }

    @Override
    public void difficultyplus$setData(DifficultyPlusData data) {
        this.difficultyplus = data;
    }
}
