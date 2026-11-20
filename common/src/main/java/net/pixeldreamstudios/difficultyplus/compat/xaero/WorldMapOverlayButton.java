package net.pixeldreamstudios.difficultyplus.compat.xaero;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.hooks.client.screen.ScreenAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.client.ZoneOverlayKey;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneStyles;
import net.pixeldreamstudios.difficultyplus.compat.MapTooltip;
import xaero.map.gui.GuiMap;

import java.util.List;

public final class WorldMapOverlayButton extends Button {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, "textures/gui/xaero_zone_toggle.png");
    private static final int SIZE = 20;
    private static final int ICON = 16;
    private static final int SHEET_WIDTH = 32;
    private static final int SHEET_HEIGHT = 16;
    private static final int SHOWN_U = 0;
    private static final int HIDDEN_U = 16;
    private static final float IDLE_TINT = 0.9882f;
    private static final float HOVER_TINT = 0.9f;
    private static final float INACTIVE_TINT = 0.25f;
    private static final int HOVER_LIFT = 1;
    private static final int FOCUS_FILL = 0x55FFFFFF;
    private static final MapTooltip.Style TIP = new MapTooltip.Style(0xC8000000, 10, 5, 6, 10, 0, 12, 10, 9, 200);
    private static final String SHOWN = "gui.difficultyplus.map_overlay_on";
    private static final String HIDDEN = "gui.difficultyplus.map_overlay_off";
    private static final String HIDDEN_TOOLTIPS = "gui.difficultyplus.map_overlay_off_tooltips";

    private WorldMapOverlayButton(int x, int y) {
        super(x, y, SIZE, SIZE, label(), WorldMapOverlayButton::press, DEFAULT_NARRATION);
    }

    public static void register() {
        ClientGuiEvent.INIT_POST.register(WorldMapOverlayButton::attach);
    }

    private static void attach(Screen screen, ScreenAccess access) {
        if (!GuiMap.class.isInstance(screen)) {
            return;
        }
        List<? extends GuiEventListener> taken = screen.children();
        for (int x = screen.width - SIZE; x >= 0; x -= SIZE) {
            for (int y = screen.height - SIZE; y >= 0; y -= SIZE) {
                if (free(taken, x, y)) {
                    access.addRenderableWidget(new WorldMapOverlayButton(x, y));
                    return;
                }
            }
        }
    }

    private static boolean free(List<? extends GuiEventListener> taken, int x, int y) {
        for (GuiEventListener listener : taken) {
            if (listener instanceof AbstractWidget widget && widget.getX() < x + SIZE && x < widget.getX() + widget.getWidth()
                    && widget.getY() < y + SIZE && y < widget.getY() + widget.getHeight()) {
                return false;
            }
        }
        return true;
    }

    private static void press(Button button) {
        ZoneStyles.toggleOverlay();
        button.setMessage(label());
    }

    private static Component label() {
        String key = ZoneStyles.overlayShown() ? SHOWN : ZoneStyles.tooltips() ? HIDDEN_TOOLTIPS : HIDDEN;
        return Component.translatable(key, ZoneOverlayKey.keyLabel());
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int iconX = getX() + width / 2 - ICON / 2;
        int iconY = getY() + height / 2 - ICON / 2;
        float tint = IDLE_TINT;
        if (!active) {
            tint = INACTIVE_TINT;
        } else if (isHovered()) {
            iconY -= HOVER_LIFT;
            tint = HOVER_TINT;
        }
        RenderSystem.setShaderColor(tint, tint, tint, 1f);
        if (isFocused()) {
            graphics.fill(iconX, iconY, iconX + ICON, iconY + ICON, FOCUS_FILL);
        }
        graphics.blit(TEXTURE, iconX, iconY, ZoneStyles.overlayShown() ? SHOWN_U : HIDDEN_U, 0, ICON, ICON, SHEET_WIDTH, SHEET_HEIGHT);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (isHovered()) {
            MapTooltip.draw(graphics, List.of(getMessage()), mouseX, mouseY, TIP);
        }
    }
}
