package net.pixeldreamstudios.difficultyplus.internal;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.DifficultyTypes;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.internal.damage.ConfigResistanceProvider;
import net.pixeldreamstudios.difficultyplus.internal.effect.ZoneEffectManager;
import java.util.List;

public final class ConfigValidator {
    private ConfigValidator() {}

    private static final Gson GSON = new Gson();

    public static void validate() {
        int effects = 0;
        int problems = 0;

        for (Config.DifficultyType type : DifficultyTypes.resolved) {
            DifficultyPlusData data = ((ConfigAccess.Type) (Object) type).difficultyplus$data();

            problems += validateResistances(type.name, "mob_resistances", data.mob_resistances);
            problems += validateResistances(type.name, "player_resistances", data.player_resistances);

            if (data.zone_effects != null) {
                for (DifficultyPlusData.ZoneEffectEntry entry : data.zone_effects) {
                    entry.raw = reserialize(entry);
                    effects++;
                    if (entry.type == null) {
                        DifficultyPlus.LOGGER.warn("Difficulty '{}' has a zone effect with no 'type'", type.name);
                        problems++;
                        continue;
                    }
                    ResourceLocation id = ResourceLocation.tryParse(entry.type);
                    if (id == null || !DifficultyPlusAPI.zoneEffects().has(id)) {
                        DifficultyPlus.LOGGER.warn(
                                "Difficulty '{}' references unknown zone effect '{}' — its mod may not be installed",
                                type.name, entry.type);
                        problems++;
                    }
                }
            }
        }

        problems += validateZoneRanges();

        ZoneEffectManager.recomputeTargets();

        if (problems > 0) {
            DifficultyPlus.LOGGER.warn("Difficulty+ loaded with {} config problem(s)", problems);
        } else if (effects > 0) {
            DifficultyPlus.LOGGER.info("Difficulty+ loaded {} zone effect(s)", effects);
        }
    }

    private static int validateResistances(String typeName,
                                           String listName,
                                           List<DifficultyPlusData.ResistanceEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return 0;
        }
        int problems = 0;
        for (DifficultyPlusData.ResistanceEntry entry : entries) {
            if (!ConfigResistanceProvider.isResolvable(entry)) {
                DifficultyPlus.LOGGER.warn(
                        "Difficulty '{}' has a {} entry with no usable damage_type or damage_tag",
                        typeName, listName);
                problems++;
            }
        }
        return problems;
    }

    private static int validateZoneRanges() {
        Config config = ConfigBridge.config();
        if (config == null || config.dimensions == null) {
            return 0;
        }

        int problems = 0;
        for (Config.Dimension dimension : config.dimensions) {
            if (dimension.zones == null) {
                continue;
            }
            for (Config.Zone zone : dimension.zones) {
                ZoneData.LevelRange range = ConfigBridge.forZone(zone).level_range;
                if (range == null) {
                    continue;
                }

                String label = zone.zone_matches == null ? "<unmatched>"
                        : (zone.zone_matches.structure != null && !zone.zone_matches.structure.isEmpty()
                        ? zone.zone_matches.structure : zone.zone_matches.biome);

                if (range.max < range.min) {
                    DifficultyPlus.LOGGER.warn(
                            "Zone '{}' has level_range min {} above max {} — it will not randomize",
                            label, range.min, range.max);
                    problems++;
                    continue;
                }
                if (!range.isValid()) {
                    DifficultyPlus.LOGGER.warn(
                            "Zone '{}' has an ABSOLUTE level_range with a negative min ({})",
                            label, range.min);
                    problems++;
                    continue;
                }
                if (range.floor != null && range.ceiling != null && range.ceiling < range.floor) {
                    DifficultyPlus.LOGGER.warn(
                            "Zone '{}' has level_range ceiling {} below floor {} — the ceiling is ignored",
                            label, range.ceiling, range.floor);
                    problems++;
                    continue;
                }

                int zoneLevel = zone.difficulty == null ? 0 : zone.difficulty.level;
                if (range.isRanged() && range.highestFor(zoneLevel) <= range.lowestFor(zoneLevel)) {
                    DifficultyPlus.LOGGER.warn(
                            "Zone '{}' has a level_range that clamps to a single level ({}) — it will not randomize",
                            label, range.lowestFor(zoneLevel));
                    problems++;
                }
            }
        }
        return problems;
    }

    private static JsonObject reserialize(DifficultyPlusData.ZoneEffectEntry entry) {
        JsonElement element = GSON.toJsonTree(entry);
        return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }
}
