package net.pixeldreamstudios.difficultyplus.compat.journeymap;

import dev.architectury.event.events.client.ClientTickEvent;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygonWithHoles;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.client.util.PolygonHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneStyles;
import net.pixeldreamstudios.difficultyplus.compat.xaero.ZoneColours;
import net.pixeldreamstudios.difficultyplus.config.ClientData;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JourneyMapZones {
    private JourneyMapZones() {}

    private static final int REFRESH_TICKS = 20;
    private static final int REGION_SHIFT = 5;
    private static final int REGION_CHUNKS = 1 << REGION_SHIFT;
    private static final int SCAN_RADIUS = 4;
    private static final int KEEP_RADIUS = SCAN_RADIUS + 1;
    private static final int OVERLAY_Y = 64;
    private static final int DISPLAY_ORDER = 900;
    private static final float OUTLINE_WIDTH = 2f;
    private static final int LEVEL_BITS = 32;
    private static final long LEVEL_MASK = 0xFFFFFFFFL;
    private static final String GROUP = "Difficulty+ zones";

    private static final Long2IntMap REGION_HASHES = new Long2IntOpenHashMap();
    private static final Long2ObjectMap<List<PolygonOverlay>> SHOWN = new Long2ObjectOpenHashMap<>();

    private static IClientAPI api;
    private static boolean registered;
    private static int ticks;
    private static ResourceLocation shownDimension;
    private static int shownStyle;
    private static boolean failed;

    public static void attach(IClientAPI clientApi) {
        api = clientApi;
        if (!registered) {
            registered = true;
            ClientTickEvent.CLIENT_POST.register(client -> tick());
        }
    }

    private static void tick() {
        if (api == null || ++ticks < REFRESH_TICKS) {
            return;
        }
        ticks = 0;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            clear();
            shownDimension = null;
            return;
        }
        ResourceLocation dimension = minecraft.level.dimension().location();
        int style = styleHash();
        if (!dimension.equals(shownDimension) || style != shownStyle) {
            clear();
            shownDimension = dimension;
            shownStyle = style;
        }
        if (!ZoneStyles.drawn(ZoneStyles.Map.WORLD_MAP) && !ZoneStyles.drawn(ZoneStyles.Map.MINIMAP)) {
            return;
        }
        ChunkPos centre = minecraft.player.chunkPosition();
        int regionX = centre.x >> REGION_SHIFT;
        int regionZ = centre.z >> REGION_SHIFT;
        for (int x = regionX - SCAN_RADIUS; x <= regionX + SCAN_RADIUS; x++) {
            for (int z = regionZ - SCAN_RADIUS; z <= regionZ + SCAN_RADIUS; z++) {
                long key = ChunkPos.asLong(x, z);
                int hash = ZoneChunkCache.regionHash(dimension, x, z);
                if (REGION_HASHES.containsKey(key) && REGION_HASHES.get(key) == hash) {
                    continue;
                }
                REGION_HASHES.put(key, hash);
                rebuild(dimension, x, z, key);
            }
        }
        dropFar(regionX, regionZ);
    }

    private static int styleHash() {
        ClientData config = ClientConfig.get();
        return Objects.hash(ZoneStyles.hash(ZoneStyles.Map.WORLD_MAP), ZoneStyles.hash(ZoneStyles.Map.MINIMAP),
                config.border_opacity, config.wash_opacity, config.opacity);
    }

    private static void rebuild(ResourceLocation dimension, int regionX, int regionZ, long key) {
        remove(key);
        Long2ObjectMap<List<ChunkPos>> groups = new Long2ObjectOpenHashMap<>();
        int minX = regionX << REGION_SHIFT;
        int minZ = regionZ << REGION_SHIFT;
        for (int x = minX; x < minX + REGION_CHUNKS; x++) {
            for (int z = minZ; z < minZ + REGION_CHUNKS; z++) {
                ZoneChunkCache.Entry entry = ZoneChunkCache.get(dimension, ChunkPos.asLong(x, z));
                if (entry == null || entry.zoneId() == 0) {
                    continue;
                }
                long group = ((long) entry.zoneId() << LEVEL_BITS) | (entry.level() & LEVEL_MASK);
                groups.computeIfAbsent(group, ignored -> new ArrayList<>()).add(new ChunkPos(x, z));
            }
        }
        if (groups.isEmpty()) {
            return;
        }
        ResourceKey<Level> level = ResourceKey.create(Registries.DIMENSION, dimension);
        List<PolygonOverlay> made = new ArrayList<>();
        for (Long2ObjectMap.Entry<List<ChunkPos>> group : groups.long2ObjectEntrySet()) {
            int zoneId = (int) (group.getLongKey() >>> LEVEL_BITS);
            int zoneLevel = (int) (group.getLongKey() & LEVEL_MASK);
            int rgb = ZoneColours.rgb(zoneId, zoneLevel);
            for (MapPolygonWithHoles polygon : PolygonHelper.createChunksPolygon(group.getValue(), OVERLAY_Y)) {
                if (ZoneStyles.drawn(ZoneStyles.Map.WORLD_MAP)) {
                    show(made, overlay(level, polygon, shape(ZoneStyles.Map.WORLD_MAP, rgb), Context.UI.Fullscreen));
                }
                if (ZoneStyles.drawn(ZoneStyles.Map.MINIMAP)) {
                    show(made, overlay(level, polygon, shape(ZoneStyles.Map.MINIMAP, rgb), Context.UI.Minimap));
                }
            }
        }
        SHOWN.put(key, made);
    }

    private static ShapeProperties shape(ZoneStyles.Map map, int rgb) {
        ShapeProperties shape = new ShapeProperties().setStrokeColor(rgb).setFillColor(rgb).setStrokeWidth(OUTLINE_WIDTH)
                .setStrokeOpacity(0f).setFillOpacity(0f);
        ClientData config = ClientConfig.get();
        float strength = ZoneStyles.strength();
        float border = Mth.clamp(config.border_opacity, 0f, 1f) * strength;
        switch (ZoneStyles.style(map)) {
            case OUTLINE -> shape.setStrokeOpacity(border);
            case WASH -> shape.setStrokeOpacity(border).setFillOpacity(Mth.clamp(config.wash_opacity, 0f, 1f) * strength);
            case FULL -> shape.setFillOpacity(Mth.clamp(config.opacity, 0f, 1f) * strength);
        }
        return shape;
    }

    private static PolygonOverlay overlay(ResourceKey<Level> level, MapPolygonWithHoles polygon, ShapeProperties shape, Context.UI ui) {
        PolygonOverlay overlay = new PolygonOverlay(DifficultyPlus.MOD_ID, level, shape, polygon);
        overlay.setOverlayGroupName(GROUP);
        overlay.setActiveUIs(ui);
        overlay.setDisplayOrder(DISPLAY_ORDER);
        return overlay;
    }

    private static void show(List<PolygonOverlay> made, PolygonOverlay overlay) {
        try {
            api.show(overlay);
            made.add(overlay);
        } catch (Exception exception) {
            if (!failed) {
                failed = true;
                DifficultyPlus.LOGGER.warn("Difficulty+ could not show a zone on JourneyMap", exception);
            }
        }
    }

    private static void remove(long key) {
        List<PolygonOverlay> shown = SHOWN.remove(key);
        if (shown != null) {
            shown.forEach(api::remove);
        }
    }

    private static void dropFar(int regionX, int regionZ) {
        List<Long> far = new ArrayList<>();
        for (long key : REGION_HASHES.keySet()) {
            ChunkPos region = new ChunkPos(key);
            if (Math.abs(region.x - regionX) > KEEP_RADIUS || Math.abs(region.z - regionZ) > KEEP_RADIUS) {
                far.add(key);
            }
        }
        for (long key : far) {
            remove(key);
            REGION_HASHES.remove(key);
        }
    }

    private static void clear() {
        if (api != null) {
            api.removeAll(DifficultyPlus.MOD_ID);
        }
        SHOWN.clear();
        REGION_HASHES.clear();
    }
}
