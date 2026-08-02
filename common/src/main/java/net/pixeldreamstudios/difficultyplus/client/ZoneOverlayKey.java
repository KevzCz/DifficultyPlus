package net.pixeldreamstudios.difficultyplus.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneRefresh;
import net.pixeldreamstudios.difficultyplus.config.ClientData;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;
import org.lwjgl.glfw.GLFW;

public final class ZoneOverlayKey {
    private ZoneOverlayKey() {}

    private static final KeyMapping TOGGLE = new KeyMapping(
            "key.difficultyplus.toggle_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.difficultyplus");

    public static void register() {
        KeyMappingRegistry.register(TOGGLE);
        ClientTickEvent.CLIENT_POST.register(client -> {
            while (TOGGLE.consumeClick()) {
                toggle(client);
            }
        });
    }

    private static void toggle(Minecraft client) {
        ClientData config = ClientConfig.get();
        config.show_overlay = !config.show_overlay;
        ClientConfig.save();

        ZoneChunkCache.markAllDirty();
        ZoneRefresh.requestPending();

        if (client.player != null) {
            client.player.displayClientMessage(Component.translatable(
                    config.show_overlay
                            ? "message.difficultyplus.overlay_on"
                            : "message.difficultyplus.overlay_off")
                    .withStyle(config.show_overlay
                            ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
        }
    }
}
