package net.pixeldreamstudios.difficultyplus.compat.journeymap;

import com.mojang.blaze3d.platform.Window;
import journeymap.api.v2.client.event.FullscreenMapEvent;
import journeymap.api.v2.client.event.FullscreenRenderEvent;
import journeymap.api.v2.common.event.FullscreenEventRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneStyles;
import net.pixeldreamstudios.difficultyplus.compat.MapTooltip;
import net.pixeldreamstudios.difficultyplus.compat.ZoneTooltips;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;

import java.util.List;

public final class JourneyMapZoneTooltip {
    private JourneyMapZoneTooltip() {}

    private static final int HOVER_SLACK = 1;
    private static final int UNSET = Integer.MIN_VALUE;
    private static final int CHUNK_SHIFT = 4;
    private static final MapTooltip.Style STYLE = new MapTooltip.Style(0x80000000, 2, 2, 2, 10, 1, 12, 10, 9, 200);

    private static boolean registered;
    private static ResourceLocation dimension;
    private static BlockPos block;
    private static int mouseX = UNSET;
    private static int mouseY = UNSET;

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        FullscreenEventRegistry.FULLSCREEN_MAP_MOVE_EVENT.subscribe(DifficultyPlus.MOD_ID, event -> hover(event.getLocation(), event.getLevel()));
        FullscreenEventRegistry.FULLSCREEN_MAP_DRAG_EVENT.subscribe(DifficultyPlus.MOD_ID, event -> hover(event.getLocation(), event.getLevel()));
        FullscreenEventRegistry.FULLSCREEN_RENDER_EVENT.subscribe(DifficultyPlus.MOD_ID, JourneyMapZoneTooltip::render);
    }

    private static void hover(BlockPos location, ResourceKey<Level> level) {
        block = location;
        dimension = level == null ? null : level.location();
        Minecraft minecraft = Minecraft.getInstance();
        Window window = minecraft.getWindow();
        MouseHandler mouse = minecraft.mouseHandler;
        mouseX = (int) (mouse.xpos() * window.getGuiScaledWidth() / window.getScreenWidth());
        mouseY = (int) (mouse.ypos() * window.getGuiScaledHeight() / window.getScreenHeight());
    }

    private static void render(FullscreenRenderEvent event) {
        if (block == null || dimension == null || !ZoneStyles.tooltips()) {
            return;
        }
        if (Math.abs(event.getMouseX() - mouseX) > HOVER_SLACK || Math.abs(event.getMouseY() - mouseY) > HOVER_SLACK) {
            return;
        }
        List<Component> lines = ZoneTooltips.lines(dimension, ChunkPos.asLong(block.getX() >> CHUNK_SHIFT, block.getZ() >> CHUNK_SHIFT),
                ZoneTooltips.quartOf(block.getX(), block.getZ()), ClientConfig.get().show_resistances);
        MapTooltip.draw(event.getGraphics(), lines, event.getMouseX(), event.getMouseY(), STYLE);
    }
}
