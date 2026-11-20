package net.pixeldreamstudios.difficultyplus.compat.journeymap;

import journeymap.api.v2.client.event.FullscreenDisplayEvent;
import journeymap.api.v2.client.fullscreen.IThemeButton;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneStyles;

public final class JourneyMapOverlayButton {
    private JourneyMapOverlayButton() {}

    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, "textures/gui/journeymap_zone_toggle.png");
    private static final String SHOWN = "gui.difficultyplus.journeymap_overlay_on";
    private static final String HIDDEN = "gui.difficultyplus.journeymap_overlay_off";
    private static final String HINT = "gui.difficultyplus.journeymap_overlay_hint";
    private static final String HINT_TOOLTIPS = "gui.difficultyplus.journeymap_overlay_hint_tooltips";

    private static boolean registered;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        FullscreenEventRegistry.ADDON_BUTTON_DISPLAY_EVENT.subscribe(DifficultyPlus.MOD_ID, JourneyMapOverlayButton::add);
    }

    private static void add(FullscreenDisplayEvent.AddonButtonDisplayEvent event) {
        IThemeButton button = event.getThemeButtonDisplay().addThemeToggleButton(
                Component.translatable(SHOWN).getString(), Component.translatable(HIDDEN).getString(), ICON, ZoneStyles.overlayShown(),
                JourneyMapOverlayButton::press);
        button.setTooltip(Component.translatable(ZoneStyles.tooltips() ? HINT_TOOLTIPS : HINT).getString());
    }

    private static void press(IThemeButton button) {
        button.setToggled(ZoneStyles.toggleOverlay());
    }
}
