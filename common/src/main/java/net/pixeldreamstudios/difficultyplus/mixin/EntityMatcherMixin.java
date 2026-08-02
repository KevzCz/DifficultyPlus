package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.config.Config;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Config.EntityMatcher.class)
public class EntityMatcherMixin implements ConfigAccess.Entity {

    public ZoneData difficultyplus = new ZoneData();

    @Override
    public ZoneData difficultyplus$entity() {
        if (difficultyplus == null) {
            difficultyplus = new ZoneData();
        }
        return difficultyplus;
    }
}
