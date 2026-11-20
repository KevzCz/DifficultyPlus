package net.pixeldreamstudios.difficultyplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneStyles;
import net.pixeldreamstudios.difficultyplus.config.ClientData;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public final class ZoneOverlayKey {
    private ZoneOverlayKey() {}

    private static final long PEEK_AFTER_MS = 250L;
    private static final long NOT_PRESSED = -1L;
    private static final String OVERLAY_ON = "message.difficultyplus.overlay_on";
    private static final String OVERLAY_OFF = "message.difficultyplus.overlay_off";
    private static final String OVERLAY_OFF_TOOLTIPS = "message.difficultyplus.overlay_off_tooltips";

    private static final KeyMapping TOGGLE = new KeyMapping(
            "key.difficultyplus.toggle_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "key.categories.difficultyplus");

    private static long pressedAt = NOT_PRESSED;
    private static boolean peeking;

    public static void register() {
        KeyMappingRegistry.register(TOGGLE);
        ClientTickEvent.CLIENT_POST.register(ZoneOverlayKey::tick);
    }

    private static void tick(Minecraft client) {
        while (TOGGLE.consumeClick()) {
            if (pressedAt == NOT_PRESSED) {
                pressedAt = Util.getMillis();
            }
        }
        boolean down = TOGGLE.isDown();
        if (down && pressedAt == NOT_PRESSED) {
            pressedAt = Util.getMillis();
        }
        if (down && !peeking && Util.getMillis() - pressedAt >= PEEK_AFTER_MS) {
            peeking = true;
            ZoneStyles.peek(true);
        }
        if (!down && pressedAt != NOT_PRESSED) {
            if (peeking) {
                peeking = false;
                ZoneStyles.peek(false);
            } else {
                toggle(client);
            }
            pressedAt = NOT_PRESSED;
        }
    }

    public static Component keyLabel() {
        return Component.literal(TOGGLE.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT)).withStyle(ChatFormatting.DARK_GREEN);
    }

    private static void toggle(Minecraft client) {
        boolean shown = ZoneStyles.toggleOverlay();
        if (client.player != null) {
            client.player.displayClientMessage(Component.translatable(message(ClientConfig.get()))
                    .withStyle(shown ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        }
    }

    private static String message(ClientData config) {
        if (config.show_overlay) {
            return OVERLAY_ON;
        }
        return config.show_tooltips ? OVERLAY_OFF_TOOLTIPS : OVERLAY_OFF;
    }
}
