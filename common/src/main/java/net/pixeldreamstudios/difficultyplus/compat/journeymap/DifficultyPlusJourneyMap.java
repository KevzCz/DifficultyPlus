package net.pixeldreamstudios.difficultyplus.compat.journeymap;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

@JourneyMapPlugin(apiVersion = DifficultyPlusJourneyMap.API_VERSION)
public final class DifficultyPlusJourneyMap implements IClientPlugin {
    public static final String API_VERSION = "2.0.0";

    @Override
    public void initialize(IClientAPI api) {
        JourneyMapZones.attach(api);
        JourneyMapOverlayButton.register();
        JourneyMapZoneTooltip.register();
    }

    @Override
    public String getModId() {
        return DifficultyPlus.MOD_ID;
    }
}
