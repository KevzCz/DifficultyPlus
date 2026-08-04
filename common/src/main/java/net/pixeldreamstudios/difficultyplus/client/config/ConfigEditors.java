package net.pixeldreamstudios.difficultyplus.client.config;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.dungeon_difficulty.config.Config;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.compat.DamageTypeNames;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.config.DimensionData;
import net.pixeldreamstudios.difficultyplus.config.DistanceScaling;
import net.pixeldreamstudios.difficultyplus.config.LevelColours;
import net.pixeldreamstudios.difficultyplus.config.LevelGapData;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigAccess;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ConfigEditors {
    private ConfigEditors() {}

    public static List<AbstractConfigListEntry<?>> meta(Config.Meta meta) {
        return Entries.of(
                Entries.toggle("sanitize_config", meta.sanitize_config,
                        v -> meta.sanitize_config = v),
                Entries.optionalDouble("rounding_unit", meta.rounding_unit,
                        v -> meta.rounding_unit = v),
                Entries.toggle("merge_item_modifiers", meta.merge_item_modifiers,
                        v -> meta.merge_item_modifiers = v));
    }

    public static List<AbstractConfigListEntry<?>> announcement(Config.Announcement value) {
        return Entries.of(
                Entries.toggle("enabled", value.enabled, v -> value.enabled = v),
                Entries.integer("check_interval_seconds", value.check_interval_seconds,
                        v -> value.check_interval_seconds = v),
                Entries.integer("history_size", value.history_size, v -> value.history_size = v));
    }

    public static List<AbstractConfigListEntry<?>> perPlayer(Config.PerPlayerDifficulty value) {
        return Entries.of(
                Entries.toggle("enabled", value.enabled, v -> value.enabled = v),
                Entries.selector("counting", Config.PerPlayerDifficulty.Counting.values(),
                        value.counting, v -> value.counting = v),
                Entries.integer("cap", value.cap, v -> value.cap = v),
                Entries.objectList("entities", value.entities, v -> value.entities = v,
                        Config.EntityModifier::new,
                        ConfigEditors::labelEntityModifier,
                        ConfigEditors::entityModifier));
    }

    public static List<AbstractConfigListEntry<?>> global(GlobalData data) {
        return Entries.of(
                Entries.integer("zone_poll_interval", data.zone_poll_interval,
                        v -> data.zone_poll_interval = v),
                Entries.decimal("resistance_reduction_cap", data.resistance_reduction_cap,
                        v -> data.resistance_reduction_cap = v),
                Entries.integer("biome_fill_budget", data.biome_fill_budget,
                        v -> data.biome_fill_budget = v),
                Entries.group("zone_map", zoneMap(data.zone_map == null
                        ? data.zone_map = new GlobalData.ZoneMap()
                        : data.zone_map)),
                Entries.group("level_gap", levelGap(data.level_gap == null
                        ? data.level_gap = new LevelGapData()
                        : data.level_gap)),
                Entries.group("late_equipment", lateEquipment(data.late_equipment == null
                        ? data.late_equipment = new GlobalData.LateEquipment()
                        : data.late_equipment)),
                Entries.group("structure_refresh", structureRefresh(data.structure_refresh)));
    }

    public static List<AbstractConfigListEntry<?>> levelGap(LevelGapData value) {
        return Entries.of(
                Entries.toggle("enabled", value.enabled, v -> value.enabled = v,
                        "Tilts combat by the gap between a player's skill level and a mob's"
                                + " level. Needs Puffish Skills."),
                Entries.stringList("categories", value.categories,
                        v -> value.categories = v),
                Entries.decimal("player_scale", value.player_scale,
                        v -> value.player_scale = v),
                Entries.group("player_below", levelGapHalf(value.player_below == null
                        ? value.player_below = new LevelGapData.Half()
                        : value.player_below)),
                Entries.group("player_above", levelGapHalf(value.player_above == null
                        ? value.player_above = new LevelGapData.Half()
                        : value.player_above)),
                Entries.group("mob_vs_mob", mobVsMob(value.mob_vs_mob == null
                        ? value.mob_vs_mob = new LevelGapData.MobVsMob()
                        : value.mob_vs_mob)));
    }

    public static List<AbstractConfigListEntry<?>> mobVsMob(LevelGapData.MobVsMob value) {
        return Entries.of(
                Entries.toggle("enabled", value.enabled, v -> value.enabled = v,
                        "Tilts mob-on-mob damage by the gap between their zone levels. Only"
                                + " dealt_per_level is used. Does not need Puffish Skills."),
                Entries.group("attacker_below", levelGapHalf(value.attacker_below == null
                        ? value.attacker_below = new LevelGapData.Half()
                        : value.attacker_below)),
                Entries.group("attacker_above", levelGapHalf(value.attacker_above == null
                        ? value.attacker_above = new LevelGapData.Half()
                        : value.attacker_above)));
    }

    public static List<AbstractConfigListEntry<?>> levelGapHalf(LevelGapData.Half value) {
        return Entries.of(
                Entries.toggle("enabled", value.enabled, v -> value.enabled = v),
                Entries.decimal("dealt_per_level", value.dealt_per_level,
                        v -> value.dealt_per_level = v),
                Entries.decimal("taken_per_level", value.taken_per_level,
                        v -> value.taken_per_level = v),
                Entries.decimal("min_multiplier", value.min_multiplier,
                        v -> value.min_multiplier = v,
                        "Floor for both multipliers. 0.25 means damage never drops below a"
                                + " quarter."),
                Entries.decimal("max_multiplier", value.max_multiplier,
                        v -> value.max_multiplier = v,
                        "Ceiling for both multipliers. 2.0 means damage never more than"
                                + " doubles."));
    }

    public static List<AbstractConfigListEntry<?>> zoneMap(GlobalData.ZoneMap value) {
        return Entries.of(
                Entries.toggle("enabled", value.enabled, v -> value.enabled = v),
                Entries.integer("send_distance", value.send_distance,
                        v -> value.send_distance = v),
                Entries.toggle("quart_detail", value.quart_detail,
                        v -> value.quart_detail = v,
                        "Resolves zones per 4x4 block cell instead of per chunk, so borders"
                                + " follow biome edges. Costs more sampling at boundaries."));
    }

    public static List<AbstractConfigListEntry<?>> structureRefresh(
            GlobalData.StructureRefresh value) {
        return Entries.of(
                Entries.toggle("enabled", value.enabled, v -> value.enabled = v),
                Entries.integer("cooldown_minutes", value.cooldown_minutes,
                        v -> value.cooldown_minutes = v),
                Entries.integer("chunks_per_tick", value.chunks_per_tick,
                        v -> value.chunks_per_tick = v),
                Entries.toggle("clear_owned_entities", value.clear_owned_entities,
                        v -> value.clear_owned_entities = v),
                Entries.stringList("blacklist", value.blacklist, v -> value.blacklist = v),
                Entries.stringList("reroll_blacklist", value.reroll_blacklist,
                        v -> value.reroll_blacklist = v),
                Entries.stringList("key_blocks", value.key_blocks, v -> value.key_blocks = v),
                Entries.stringList("never_clear", value.never_clear, v -> value.never_clear = v),
                Entries.group("reliquary_drops", Entries.of(
                        Entries.decimal("hollow_skull_chance", value.hollow_skull_chance,
                                v -> value.hollow_skull_chance = v),
                        Entries.integer("hollow_skull_min_level", value.hollow_skull_min_level,
                                v -> value.hollow_skull_min_level = v),
                        Entries.decimal("sapphire_chance", value.sapphire_chance,
                                v -> value.sapphire_chance = v),
                        Entries.integer("sapphire_min_level", value.sapphire_min_level,
                                v -> value.sapphire_min_level = v),
                        Entries.decimal("garnet_chance", value.garnet_chance,
                                v -> value.garnet_chance = v),
                        Entries.integer("garnet_min_level", value.garnet_min_level,
                                v -> value.garnet_min_level = v))));
    }

    public static List<AbstractConfigListEntry<?>> difficultyType(Config.DifficultyType type) {
        DifficultyPlusData ours = ((ConfigAccess.Type) (Object) type).difficultyplus$data();
        return Entries.of(
                Entries.requiredString("name", type.name, v -> type.name = v),
                Entries.string("parent", type.parent, v -> type.parent = v),
                Entries.string("translation_code", type.translation_code,
                        v -> type.translation_code = v),
                Entries.optionalToggle("allow_loot_scaling", type.allow_loot_scaling,
                        v -> type.allow_loot_scaling = v),
                Entries.objectList("entities", type.entities, v -> type.entities = v,
                        Config.EntityModifier::new,
                        ConfigEditors::labelEntityModifier,
                        ConfigEditors::entityModifier),
                Entries.group("difficultyplus", Entries.of(
                        Entries.objectList("mob_resistances", ours.mob_resistances,
                                v -> ours.mob_resistances = v,
                                DifficultyPlusData.ResistanceEntry::new,
                                ConfigEditors::labelResistance,
                                ConfigEditors::resistance),
                        Entries.objectList("player_resistances", ours.player_resistances,
                                v -> ours.player_resistances = v,
                                DifficultyPlusData.ResistanceEntry::new,
                                ConfigEditors::labelResistance,
                                ConfigEditors::playerResistance),
                        Entries.objectList("zone_effects", ours.zone_effects,
                                v -> ours.zone_effects = v,
                                DifficultyPlusData.ZoneEffectEntry::new,
                                e -> e.type == null || e.type.isEmpty() ? "(effect)" : e.type,
                                ConfigEditors::zoneEffect))));
    }

    public static List<AbstractConfigListEntry<?>> entityModifier(Config.EntityModifier value) {
        if (value.entity_matches == null) {
            value.entity_matches = new Config.EntityModifier.Filters();
        }
        if (value.spawners == null) {
            value.spawners = new Config.SpawnerModifier();
        }
        Config.EntityModifier.Filters filters = value.entity_matches;
        return Entries.of(
                Entries.selector("attitude", Config.EntityModifier.Filters.Attitude.values(),
                        filters.attitude, v -> filters.attitude = v),
                Entries.searchable("type", Suggestions::entityTypes,
                        filters.type, v -> filters.type = v,
                        "An entity id, a #tag, or a regex. Empty matches all."),
                Entries.decimal("experience_multiplier", value.experience_multiplier,
                        v -> value.experience_multiplier = v),
                Entries.group("spawners", spawners(value.spawners)),
                Entries.objectList("attributes", value.attributes, v -> value.attributes = v,
                        Config.AttributeModifier::new,
                        ConfigEditors::labelAttribute,
                        ConfigEditors::attribute));
    }

    public static List<AbstractConfigListEntry<?>> spawners(Config.SpawnerModifier value) {
        return Entries.of(
                Entries.decimal("spawn_range_multiplier", value.spawn_range_multiplier,
                        v -> value.spawn_range_multiplier = v),
                Entries.decimal("spawn_count_multiplier", value.spawn_count_multiplier,
                        v -> value.spawn_count_multiplier = v),
                Entries.decimal("max_nearby_entities_multiplier", value.max_nearby_entities_multiplier,
                        v -> value.max_nearby_entities_multiplier = v),
                Entries.decimal("min_spawn_delay_multiplier", value.min_spawn_delay_multiplier,
                        v -> value.min_spawn_delay_multiplier = v),
                Entries.decimal("max_spawn_delay_multiplier", value.max_spawn_delay_multiplier,
                        v -> value.max_spawn_delay_multiplier = v),
                Entries.decimal("required_player_range_multiplier", value.required_player_range_multiplier,
                        v -> value.required_player_range_multiplier = v));
    }

    public static List<AbstractConfigListEntry<?>> attribute(Config.AttributeModifier value) {
        return Entries.of(
                Entries.searchable("attribute", Suggestions::attributes,
                        value.attribute, v -> value.attribute = v),
                Entries.selector("operation", Config.Operation.values(), value.operation,
                        v -> value.operation = v),
                Entries.decimal("value", value.value, v -> value.value = v),
                Entries.decimal("randomness", value.randomness, v -> value.randomness = v),
                Entries.decimal("offset", value.offset, v -> value.offset = v));
    }

    public static List<AbstractConfigListEntry<?>> dimension(Config.Dimension value) {
        if (value.world_matches == null) {
            value.world_matches = new Config.Dimension.Filters();
        }
        if (value.difficulty == null) {
            value.difficulty = new Config.DifficultyReference();
        }
        Config.Dimension.Filters filters = value.world_matches;
        return Entries.of(
                Entries.searchable("dimension", Suggestions::dimensions,
                        filters.dimension, v -> filters.dimension = v,
                        "A dimension id, or a regex."),
                Entries.group("difficulty", difficultyReference(value.difficulty)),

                Entries.group("difficultyplus", Entries.of(
                        Entries.group("distance_scaling", distanceScaling(
                                dimensionData(value).distance_scaling, true)))),

                Entries.objectList("zones", value.zones, v -> value.zones = v,
                        Config.Zone::new, ConfigEditors::labelZone, ConfigEditors::zone, true),

                Entries.objectList("zone_specifiers", value.zone_specifiers,
                        v -> value.zone_specifiers = v,
                        Config.Zone.TypeOverride::new,
                        z -> z.difficulty_name == null || z.difficulty_name.isEmpty()
                                ? "(override)" : z.difficulty_name,
                        ConfigEditors::zoneSpecifier, true),
                Entries.objectList("entities", value.entities, v -> value.entities = v,
                        Config.EntityMatcher::new,
                        ConfigEditors::labelEntityMatcher,
                        ConfigEditors::entityMatcher));
    }

    public static List<AbstractConfigListEntry<?>> difficultyReference(Config.DifficultyReference value) {
        return Entries.of(
                Entries.requiredString("name", value.name, v -> value.name = v),
                Entries.integer("level", value.level, v -> value.level = v),
                Entries.optionalInt("entity_level", value.entity_level, v -> value.entity_level = v),
                Entries.optionalInt("reward_level", value.reward_level, v -> value.reward_level = v));
    }

    public static List<AbstractConfigListEntry<?>> zone(Config.Zone value) {
        if (value.zone_matches == null) {
            value.zone_matches = new Config.Zone.Filters();
        }
        if (value.difficulty == null) {
            value.difficulty = new Config.DifficultyReference();
        }
        Config.Zone.Filters filters = value.zone_matches;
        ZoneData ours = ((ConfigAccess.Zone) (Object) value).difficultyplus$zone();
        return Entries.of(
                Entries.searchable("structure", Suggestions::structuresAndTags,
                        filters.structure, v -> filters.structure = v,
                        "A structure id, a #tag, or a regex."),
                Entries.searchable("biome", Suggestions::biomesAndTags,
                        filters.biome, v -> filters.biome = v,
                        "A biome id, a #tag, or a regex."),
                Entries.group("difficulty", difficultyReference(value.difficulty)),
                Entries.group("difficultyplus", Entries.of(
                        Entries.group("level_range", levelRange(ours.level_range)),
                        Entries.group("distance_scaling", distanceScaling(
                                ours.distance_scaling == null
                                        ? ours.distance_scaling = new DistanceScaling()
                                        : ours.distance_scaling)))));
    }

    public static List<AbstractConfigListEntry<?>> zoneSpecifier(Config.Zone.TypeOverride value) {
        if (value.zone_matches == null) {
            value.zone_matches = new Config.Zone.Filters();
        }
        Config.Zone.Filters filters = value.zone_matches;
        return Entries.of(
                Entries.searchable("structure", Suggestions::structuresAndTags,
                        filters.structure, v -> filters.structure = v,
                        "A structure id, a #tag, or a regex."),
                Entries.searchable("biome", Suggestions::biomesAndTags,
                        filters.biome, v -> filters.biome = v,
                        "A biome id, a #tag, or a regex."),
                Entries.requiredString("difficulty_name", value.difficulty_name,
                        v -> value.difficulty_name = v));
    }

    public static List<AbstractConfigListEntry<?>> entityMatcher(Config.EntityMatcher value) {
        if (value.difficulty == null) {
            value.difficulty = new Config.DifficultyReference();
        }
        ZoneData ours = ((ConfigAccess.Entity) (Object) value).difficultyplus$entity();
        return Entries.of(
                Entries.searchable("entity_type", Suggestions::entityTypes,
                        value.entity_type, v -> value.entity_type = v,
                        "An entity id, a #tag, or a regex."),
                Entries.string("loot_table", value.loot_table, v -> value.loot_table = v),
                Entries.group("difficulty", difficultyReference(value.difficulty)),
                Entries.group("difficultyplus", Entries.of(
                        Entries.group("level_range", levelRange(ours.level_range)))));
    }

    public static List<AbstractConfigListEntry<?>> rewards(Config.Rewards value) {
        if (value.smithing_upgrade == null) {
            value.smithing_upgrade = new Config.Rewards.SmithingUpgrade();
        }
        Config.Rewards.SmithingUpgrade upgrade = value.smithing_upgrade;
        return Entries.of(
                Entries.objectList("armor", value.armor, v -> value.armor = v,
                        Config.ItemModifier::new, ConfigEditors::labelItemModifier,
                        ConfigEditors::itemModifier),
                Entries.objectList("weapons", value.weapons, v -> value.weapons = v,
                        Config.ItemModifier::new, ConfigEditors::labelItemModifier,
                        ConfigEditors::itemModifier),
                Entries.group("smithing_upgrade", Entries.of(
                        Entries.toggle("enabled", upgrade.enabled, v -> upgrade.enabled = v),
                        Entries.integer("add_upon_upgrade", upgrade.add_upon_upgrade,
                                v -> upgrade.add_upon_upgrade = v),
                        Entries.decimal("multiply_upon_upgrade", upgrade.multiply_upon_upgrade,
                                v -> upgrade.multiply_upon_upgrade = v))));
    }

    public static List<AbstractConfigListEntry<?>> itemModifier(Config.ItemModifier value) {
        if (value.item_matches == null) {
            value.item_matches = new Config.ItemModifier.Filters();
        }
        Config.ItemModifier.Filters filters = value.item_matches;
        return Entries.of(
                Entries.searchable("id", Suggestions::items, filters.id, v -> filters.id = v,
                        "An item id, or a regex."),
                Entries.string("loot_table_regex", filters.loot_table_regex,
                        v -> filters.loot_table_regex = v),
                Entries.string("rarity_regex", filters.rarity_regex, v -> filters.rarity_regex = v),
                Entries.objectList("attributes", value.attributes, v -> value.attributes = v,
                        Config.AttributeModifier::new, ConfigEditors::labelAttribute,
                        ConfigEditors::attribute));
    }

    public static List<AbstractConfigListEntry<?>> lateEquipment(GlobalData.LateEquipment config) {
        return Entries.of(
                Entries.toggle("enabled", config.enabled, v -> config.enabled = v),
                Entries.integer("scale_within_ticks", config.scale_within_ticks,
                        v -> config.scale_within_ticks = v));
    }

    private static DimensionData dimensionData(Config.Dimension dimension) {
        DimensionData data = ConfigBridge.forDimension(dimension);
        if (data.distance_scaling == null) {
            data.distance_scaling = new DistanceScaling();
        }
        return data;
    }

    public static List<AbstractConfigListEntry<?>> levelStop(LevelColours.Stop value) {
        return Entries.of(
                Entries.integer("level", value.level, v -> value.level = v),
                Entries.colour("colour", value.colour, v -> value.colour = v));
    }

    public static List<AbstractConfigListEntry<?>> distanceScaling(DistanceScaling value) {
        return distanceScaling(value, false);
    }

    public static List<AbstractConfigListEntry<?>> distanceScaling(DistanceScaling value,
                                                                   boolean dimensionLevel) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
        entries.add(Entries.toggle("enabled", value.enabled, v -> value.enabled = v,
                "Adds levels with distance from world spawn."));

        if (dimensionLevel) {
            entries.add(Entries.toggle("scale_fallback", value.scale_fallback,
                    v -> value.scale_fallback = v,
                    "Also scale positions where no zone matched, using this dimension's"
                            + " own difficulty."));
        } else {
            entries.add(Entries.toggle("override", value.override, v -> value.override = v,
                    "Use this block instead of the dimension's, even when disabled."
                            + " This is how a zone opts out."));
        }

        entries.add(Entries.integer("grace_radius", value.grace_radius,
                v -> value.grace_radius = v));
        entries.add(Entries.integer("per_blocks", value.per_blocks, v -> value.per_blocks = v));
        entries.add(Entries.integer("add_level", value.add_level, v -> value.add_level = v));
        entries.add(Entries.integer("max_add", value.max_add, v -> value.max_add = v));
        return entries;
    }

    public static List<AbstractConfigListEntry<?>> levelRange(ZoneData.LevelRange range) {
        return Entries.of(
                Entries.selector("mode", ZoneData.LevelRange.Mode.values(), range.mode,
                        v -> range.mode = v),
                Entries.integer("min", range.min, v -> range.min = v),
                Entries.integer("max", range.max, v -> range.max = v),
                Entries.optionalInt("floor", range.floor, v -> range.floor = v),
                Entries.optionalInt("ceiling", range.ceiling, v -> range.ceiling = v),
                Entries.integer("region_size", range.region_size, v -> range.region_size = v),
                Entries.toggle("biome_fill", range.biome_fill, v -> range.biome_fill = v,
                        "Experimental. One level per contiguous biome patch instead of a"
                                + " region_size grid. Falls back to the grid when capped."),
                Entries.integer("biome_fill_cap", range.biome_fill_cap,
                        v -> range.biome_fill_cap = v),
                Entries.toggle("lock_on_generation", range.lock_on_generation,
                        v -> range.lock_on_generation = v));
    }

    public static List<AbstractConfigListEntry<?>> resistance(DifficultyPlusData.ResistanceEntry value) {
        return Entries.of(
                Entries.searchable("damage_type", Suggestions::damageTypes,
                        value.damage_type, v -> value.damage_type = v,
                        "A damage type id, or a regex matching several."),
                Entries.searchable("damage_tag", Suggestions::damageTypeTags,
                        value.damage_tag, v -> value.damage_tag = v,
                        "A damage type tag, with or without the leading #."),
                Entries.string("display_key", value.display_key, v -> value.display_key = v,
                        "Translation key for the name shown in tooltips. Falls back to display_text."),
                Entries.string("display_text", value.display_text, v -> value.display_text = v,
                        "Literal name shown in tooltips when display_key is unset or missing."),
                Entries.decimal("percent", value.percent, v -> value.percent = v),
                Entries.decimal("flat", value.flat, v -> value.flat = v),
                Entries.integer("min_level", value.min_level, v -> value.min_level = v),
                Entries.integer("max_level", value.max_level, v -> value.max_level = v),
                Entries.selector("scope", DifficultyPlusData.ResistanceEntry.Scope.values(),
                        value.scope, v -> value.scope = v),
                Entries.group("targets", targets(value.targets == null
                        ? value.targets = new DifficultyPlusData.TargetFilter()
                        : value.targets)));
    }

    public static List<AbstractConfigListEntry<?>> playerResistance(
            DifficultyPlusData.ResistanceEntry value) {
        return Entries.of(
                Entries.searchable("damage_type", Suggestions::damageTypes,
                        value.damage_type, v -> value.damage_type = v,
                        "A damage type id, or a regex matching several."),
                Entries.searchable("damage_tag", Suggestions::damageTypeTags,
                        value.damage_tag, v -> value.damage_tag = v,
                        "A damage type tag, with or without the leading #."),
                Entries.string("display_key", value.display_key, v -> value.display_key = v,
                        "Translation key for the name shown in tooltips. Falls back to display_text."),
                Entries.string("display_text", value.display_text, v -> value.display_text = v,
                        "Literal name shown in tooltips when display_key is unset or missing."),
                Entries.decimal("percent", value.percent, v -> value.percent = v),
                Entries.decimal("flat", value.flat, v -> value.flat = v),
                Entries.integer("min_level", value.min_level, v -> value.min_level = v),
                Entries.integer("max_level", value.max_level, v -> value.max_level = v),
                Entries.group("targets", targets(value.targets == null
                        ? value.targets = new DifficultyPlusData.TargetFilter()
                        : value.targets)));
    }

    public static List<AbstractConfigListEntry<?>> zoneEffect(DifficultyPlusData.ZoneEffectEntry value) {
        List<AbstractConfigListEntry<?>> entries = new ArrayList<>(Entries.of(
                Entries.reactiveChoice("type",
                        DifficultyPlusAPI.zoneEffects().ids()
                                .stream().map(Object::toString).toList(),
                        value.type, v -> value.type = v)));

        entries.addAll(typeFields(value));

        entries.add(Entries.integer("min_level", value.min_level, v -> value.min_level = v));
        entries.add(Entries.integer("max_level", value.max_level, v -> value.max_level = v));
        entries.add(Entries.group("targets", targets(value.targets == null
                ? value.targets = new DifficultyPlusData.TargetFilter()
                : value.targets)));
        return entries;
    }

    private static List<AbstractConfigListEntry<?>> typeFields(
            DifficultyPlusData.ZoneEffectEntry value) {
        String type = value.type == null ? "" : value.type;
        return switch (type) {
            case "difficultyplus:attribute" -> Entries.of(
                    Entries.searchable("attribute", Suggestions::attributes,
                            value.attribute, v -> value.attribute = v),
                    Entries.choice("operation",
                            List.of("ADD_VALUE", "MULTIPLY_BASE", "MULTIPLY_TOTAL"),
                            value.operation == null ? "ADD_VALUE" : value.operation,
                            v -> value.operation = v),
                    Entries.optionalDouble("value", value.value, v -> value.value = v));
            case "difficultyplus:status_effect" -> Entries.of(
                    Entries.searchable("effect", Suggestions::mobEffects,
                            value.effect, v -> value.effect = v),
                    Entries.optionalInt("amplifier", value.amplifier,
                            v -> value.amplifier = v),
                    Entries.integer("duration", value.duration, v -> value.duration = v),
                    Entries.toggle("linger", value.linger, v -> value.linger = v));
            case "difficultyplus:damage_over_time" -> Entries.of(
                    Entries.searchable("damage_type", Suggestions::damageTypes,
                            value.damage_type, v -> value.damage_type = v),
                    Entries.optionalDouble("amount",
                            value.amount == null ? null : value.amount.doubleValue(),
                            v -> value.amount = v == null ? null : v.floatValue()),
                    Entries.integer("interval", value.interval, v -> value.interval = v));
            default -> List.of();
        };
    }

    public static List<AbstractConfigListEntry<?>> targets(DifficultyPlusData.TargetFilter filter) {
        return Entries.of(
                Entries.choice("attitude", List.of("ANY", "HOSTILE", "FRIENDLY"),
                        filter.attitude, v -> filter.attitude = v),
                Entries.searchable("type", Suggestions::entityTypes,
                        filter.type, v -> filter.type = v,
                        "An entity id, a #tag, or a regex. Empty matches all."));
    }

    private static String labelZone(Config.Zone zone) {
        if (zone != null && zone.zone_matches != null) {
            if (notEmpty(zone.zone_matches.structure)) {
                return zone.zone_matches.structure;
            }
            if (notEmpty(zone.zone_matches.biome)) {
                return zone.zone_matches.biome;
            }
        }
        return "(zone)";
    }

    private static String labelEntityMatcher(Config.EntityMatcher value) {
        return value != null && notEmpty(value.entity_type) ? value.entity_type : "(entity)";
    }

    private static String labelEntityModifier(Config.EntityModifier value) {
        if (value != null && value.entity_matches != null && notEmpty(value.entity_matches.type)) {
            return value.entity_matches.type;
        }
        return "(any entity)";
    }

    private static String labelAttribute(Config.AttributeModifier value) {
        return value != null && notEmpty(value.attribute) ? value.attribute : "(attribute)";
    }

    private static String labelItemModifier(Config.ItemModifier value) {
        if (value != null && value.item_matches != null) {
            if (notEmpty(value.item_matches.id)) {
                return value.item_matches.id;
            }
            if (notEmpty(value.item_matches.loot_table_regex)) {
                return value.item_matches.loot_table_regex;
            }
        }
        return "(any item)";
    }

    private static String labelResistance(DifficultyPlusData.ResistanceEntry value) {
        if (value == null) {
            return "(resistance)";
        }
        boolean isTag = notEmpty(value.damage_tag);
        String raw = isTag ? value.damage_tag : value.damage_type;
        if (!notEmpty(raw)) {
            return "(resistance)";
        }
        ResourceLocation id = ResourceLocation.tryParse(
                raw.startsWith("#") ? raw.substring(1) : raw);
        if (id == null) {
            return raw;
        }
        return DamageTypeNames.of(id, isTag, value.display_key, value.display_text).getString();
    }

    private static boolean notEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    public static List<Config.Dimension> asList(Config.Dimension[] array) {
        List<Config.Dimension> list = new ArrayList<>();
        if (array != null) {
            Collections.addAll(list, array);
        }
        return list;
    }
}
