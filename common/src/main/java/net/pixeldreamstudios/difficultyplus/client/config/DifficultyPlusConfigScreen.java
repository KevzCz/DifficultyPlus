package net.pixeldreamstudios.difficultyplus.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.dungeon_difficulty.config.Config;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.pixeldreamstudios.difficultyplus.config.ClientData;
import net.pixeldreamstudios.difficultyplus.config.LevelColours;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.config.ConfigSaver;

import java.util.List;

public final class DifficultyPlusConfigScreen {
    private DifficultyPlusConfigScreen() {}

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
                v -> client.show_overlay = v));
        category.addEntry(Entries.toggle("show_resistances", client.show_resistances,
                v -> client.show_resistances = v));
        category.addEntry(Entries.toggle("persist", client.persist, v -> client.persist = v));
        category.addEntry(Entries.decimal("opacity", client.opacity, v -> client.opacity = v));
        category.addEntry(Entries.toggle("smooth_borders", client.smooth_borders,
                v -> client.smooth_borders = v,
                "Fades between neighbouring zone colours instead of stepping at each"
                        + " 4-block cell. Off gives hard edges."));
        category.addEntry(Entries.toggle("colour_by_level", client.colour_by_level,
                v -> client.colour_by_level = v,
                "Colours the map by level using the ramp below instead of by"
                        + " difficulty type."));
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
