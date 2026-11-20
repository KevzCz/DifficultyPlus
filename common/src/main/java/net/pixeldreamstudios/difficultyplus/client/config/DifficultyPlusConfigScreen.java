package net.pixeldreamstudios.difficultyplus.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.dungeon_difficulty.config.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
import net.pixeldreamstudios.difficultyplus.compat.xaero.ZoneColours;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneRefresh;
import net.pixeldreamstudios.difficultyplus.config.ClientData;
import net.pixeldreamstudios.difficultyplus.config.LevelColours;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.config.ConfigSaver;

import java.util.List;

public final class DifficultyPlusConfigScreen {
    private DifficultyPlusConfigScreen() {}

    private static final String SHOW_OVERLAY_TIP = "Master switch for drawing zones on both maps. The O key flips this."
            + " Zone tooltips follow show_tooltips, not this.";
    private static final String OVERLAY_OPACITY_TIP = "Fades the whole zone overlay on both maps, whatever the style."
            + " 1 is full strength, 0.3 is faint. Holding O always shows it at full strength.";
    private static final String BORDER_OPACITY_TIP = "OUTLINE and WASH styles: how solid the line around each zone is.";
    private static final String STEP_OPACITY_TIP = "OUTLINE and WASH styles: the dashed line where the level crosses"
            + " 10, 20, 30 inside a zone. 0 hides it.";
    private static final String WASH_OPACITY_TIP = "WASH style only: the light tint inside each zone.";
    private static final String FULL_OPACITY_TIP = "FULL style only, also used while holding O: how strong the"
            + " colour over every chunk is.";
    private static final String MINIMAP_OVERLAY_TIP = "Draw zones on Xaero's Minimap.";
    private static final String WORLD_MAP_OVERLAY_TIP = "Draw zones on Xaero's World Map.";
    private static final String SHOW_TOOLTIPS_TIP = "Show the zone name and level when hovering the map, even when"
            + " no zones are drawn. Turn both map overlays off and keep this on for tooltips only.";
    private static final String SMOOTHING_TIP = "Cleans up ragged zone edges on the map: a 4-block cell that is"
            + " mostly surrounded by another zone is drawn as that zone. 0 is off, 3 is the strongest."
            + " Tooltips still show the real zone.";

    public static Screen create(Screen parent) {
        Config config = ConfigBridge.config();
        if (config == null) {

            return parent;
        }

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Dungeon Difficulty"))
                .setSavingRunnable(() -> {
                    ClientConfig.save();
                    ConfigSaver.saveAndReload();
                    ZoneRefresh.requestFullRedraw();
                });

        general(builder, config);
        mapOverlay(builder);
        difficultyTypes(builder, config);
        dimensions(builder, config);
        lootScaling(builder, config);

        return builder.build();
    }

    private static void general(ConfigBuilder builder, Config config) {
        ConfigCategory category = builder.getOrCreateCategory(Component.literal("General"));
        if (config.meta != null) {
            category.addEntry(Entries.group("meta", ConfigEditors.meta(config.meta)));
        }
        if (config.announcement != null) {
            category.addEntry(Entries.group("announcement",
                    ConfigEditors.announcement(config.announcement)));
        }
        if (config.per_player_difficulty != null) {
            category.addEntry(Entries.group("per_player_difficulty",
                    ConfigEditors.perPlayer(config.per_player_difficulty)));
        }
        category.addEntry(Entries.group("difficultyplus",
                ConfigEditors.global(ConfigBridge.global())));
    }

    private static void mapOverlay(ConfigBuilder builder) {
        ClientData client = ClientConfig.get();
        ConfigCategory category = builder.getOrCreateCategory(Component.literal("Map Overlay"));
        category.addEntry(Entries.toggle("show_overlay", client.show_overlay,
                v -> client.show_overlay = v, SHOW_OVERLAY_TIP));
        category.addEntry(Entries.decimal("overlay_opacity", client.overlay_opacity,
                v -> client.overlay_opacity = Mth.clamp(v, 0f, 1f), OVERLAY_OPACITY_TIP));
        category.addEntry(Entries.toggle("minimap_overlay", client.minimap_overlay,
                v -> client.minimap_overlay = v, MINIMAP_OVERLAY_TIP));
        category.addEntry(Entries.toggle("world_map_overlay", client.world_map_overlay,
                v -> client.world_map_overlay = v, WORLD_MAP_OVERLAY_TIP));
        category.addEntry(Entries.toggle("show_tooltips", client.show_tooltips,
                v -> client.show_tooltips = v, SHOW_TOOLTIPS_TIP));
        category.addEntry(Entries.toggle("show_resistances", client.show_resistances,
                v -> client.show_resistances = v));
        category.addEntry(Entries.toggle("persist", client.persist, v -> client.persist = v));
        category.addEntry(Entries.selector("minimap_style", ClientData.OverlayStyle.values(), client.minimapStyle(),
                v -> client.minimap_style = v));
        category.addEntry(Entries.selector("world_map_style", ClientData.OverlayStyle.values(), client.worldMapStyle(),
                v -> client.world_map_style = v));
        category.addEntry(Entries.selector("colour_mode", ClientData.ColourMode.values(), client.colourMode(), v -> {
            client.colour_mode = v;
            client.colour_by_level = v == ClientData.ColourMode.LEVEL;
        }));
        category.addEntry(Entries.decimal("border_opacity", client.border_opacity,
                v -> client.border_opacity = Mth.clamp(v, 0f, 1f), BORDER_OPACITY_TIP));
        category.addEntry(Entries.decimal("step_opacity", client.step_opacity,
                v -> client.step_opacity = Mth.clamp(v, 0f, 1f), STEP_OPACITY_TIP));
        category.addEntry(Entries.decimal("wash_opacity", client.wash_opacity,
                v -> client.wash_opacity = Mth.clamp(v, 0f, 1f), WASH_OPACITY_TIP));
        category.addEntry(Entries.decimal("full_style_opacity", client.opacity,
                v -> client.opacity = Mth.clamp(v, 0f, 1f), FULL_OPACITY_TIP));
        category.addEntry(Entries.toggle("smooth_borders", client.smooth_borders,
                v -> client.smooth_borders = v,
                "FULL style only: fades between neighbouring zone colours instead of stepping at each"
                        + " 4-block cell. Off gives hard edges."));
        category.addEntry(Entries.integer("smoothing", client.smoothing,
                v -> client.smoothing = Mth.clamp(v, 0, ZoneColours.MAX_SMOOTHING), SMOOTHING_TIP));
        category.addEntry(Entries.integer("relative_safe_gap", client.relative_safe_gap, v -> client.relative_safe_gap = v));
        category.addEntry(Entries.integer("relative_warn_gap", client.relative_warn_gap, v -> client.relative_warn_gap = v));
        category.addEntry(Entries.integer("relative_danger_gap", client.relative_danger_gap, v -> client.relative_danger_gap = v));
        category.addEntry(Entries.integer("relative_cleared_gap", client.relative_cleared_gap, v -> client.relative_cleared_gap = v));
        category.addEntry(Entries.objectList("level_colours", client.level_colours,
                v -> client.level_colours = v,
                LevelColours.Stop::new,
                stop -> stop.level + "  " + (stop.colour == null ? "" : stop.colour),
                ConfigEditors::levelStop));
    }

    private static void difficultyTypes(ConfigBuilder builder, Config config) {
        ConfigCategory category = builder.getOrCreateCategory(Component.literal("Difficulty Types"));
        category.addEntry(Entries.objectList(
                "difficulty_types",
                config.difficulty_types,
                v -> config.difficulty_types = v,
                Config.DifficultyType::new,
                type -> type.name == null || type.name.isEmpty() ? "(unnamed)" : type.name,
                ConfigEditors::difficultyType));
    }

    private static void dimensions(ConfigBuilder builder, Config config) {
        ConfigCategory category = builder.getOrCreateCategory(Component.literal("Dimensions"));

        List<Config.Dimension> dimensions = ConfigEditors.asList(config.dimensions);

        category.addEntry(Entries.objectList(
                "dimensions",
                dimensions,

                v -> config.dimensions = v.toArray(new Config.Dimension[0]),
                Config.Dimension::new,
                DifficultyPlusConfigScreen::labelDimension,
                ConfigEditors::dimension,
                () -> config.dimensions = dimensions.toArray(new Config.Dimension[0])));
    }

    private static void lootScaling(ConfigBuilder builder, Config config) {
        ConfigCategory category = builder.getOrCreateCategory(Component.literal("Loot Scaling"));
        if (config.loot_scaling == null) {
            config.loot_scaling = new Config.Rewards();
        }
        for (var entry : ConfigEditors.rewards(config.loot_scaling)) {
            category.addEntry(entry);
        }
    }

    private static String labelDimension(Config.Dimension dimension) {
        if (dimension.world_matches != null
                && dimension.world_matches.dimension != null
                && !dimension.world_matches.dimension.isEmpty()) {
            return dimension.world_matches.dimension;
        }
        return "(dimension)";
    }
}
