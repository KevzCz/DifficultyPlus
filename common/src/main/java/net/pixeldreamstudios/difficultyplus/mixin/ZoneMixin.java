package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.config.Config;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Config.Zone.class)
public class ZoneMixin implements ConfigAccess.Zone {

    public ZoneData difficultyplus = new ZoneData();

    @Override
    public ZoneData difficultyplus$zone() {
        if (difficultyplus == null) {
            difficultyplus = new ZoneData();
        }
        return difficultyplus;
    }
}
