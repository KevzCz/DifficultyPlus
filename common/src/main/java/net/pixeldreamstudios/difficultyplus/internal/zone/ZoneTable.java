package net.pixeldreamstudios.difficultyplus.internal.zone;

import net.dungeon_difficulty.config.Config;
import net.minecraft.util.Mth;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.config.DistanceScaling;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigAccess;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.damage.ConfigResistanceProvider;
import net.pixeldreamstudios.difficultyplus.network.ZoneInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ZoneTable {
    private ZoneTable() {}

    private static final Map<String, Integer> IDS = new HashMap<>();

    private static List<ZoneInfo> zones = List.of();

    private static int configHash;

    public static void rebuild() {
        IDS.clear();
        List<ZoneInfo> built = new ArrayList<>();
        Config config = ConfigBridge.config();

        int next = 1;
        if (config != null && config.difficulty_types != null) {
            for (Config.DifficultyType type : config.difficulty_types) {
                if (type == null || type.name == null || type.name.isEmpty()) {
                    continue;
                }
                if (IDS.containsKey(type.name)) {
                    continue;
                }
                if (next > 255) {
                    break;
                }
                int id = next++;
                IDS.put(type.name, id);
                built.add(new ZoneInfo(id, type.name, colourFor(type.name),
                        resistancesOf(type.name)));
            }
        }
        zones = List.copyOf(built);
        configHash = computeHash();
    }

    public static List<ZoneInfo> zones() {
        return zones;
    }

    public static int configHash() {
        return configHash;
    }

    public static int idOf(String zoneName) {
        Integer id = IDS.get(zoneName);
        return id == null ? 0 : id;
    }

    private static List<ZoneInfo.Resistance> resistancesOf(String zoneName) {
        List<ZoneInfo.Resistance> out = new ArrayList<>();
        for (DifficultyPlusAPI.ResistanceInfo info
                : ConfigResistanceProvider.describeZone(zoneName, null)) {
            out.add(new ZoneInfo.Resistance(info.damageId(), info.isTag(),
                    info.percent(), info.flat(), info.displayKey(), info.displayText(),
                    info.minLevel(), info.maxLevel()));
        }
        return List.copyOf(out);
    }

    private static int colourFor(String zoneName) {
        float hue = Math.abs(zoneName.hashCode() % 360) / 360f;
        return Mth.hsvToRgb(hue, 0.55f, 0.95f) & 0x00FFFFFF;
    }

    private static int rangeHash(Config.Zone zone) {
        ZoneData data = ((ConfigAccess.Zone) (Object) zone).difficultyplus$zone();
        if (data == null || data.level_range == null) {
            return 0;
        }
        ZoneData.LevelRange range = data.level_range;
        int hash = range.mode == null ? 0 : range.mode.hashCode();
        hash = hash * 31 + range.min;
        hash = hash * 31 + range.max;
        hash = hash * 31 + (range.floor == null ? 0 : range.floor);
        hash = hash * 31 + (range.ceiling == null ? 0 : range.ceiling);
        hash = hash * 31 + range.region_size;
        hash = hash * 31 + (range.biome_fill ? 1 : 0);
        hash = hash * 31 + range.biome_fill_cap;
        hash = hash * 31 + scalingHash(data.distance_scaling);
        return hash;
    }

    private static int scalingHash(DistanceScaling scaling) {
        if (scaling == null) {
            return 0;
        }
        int hash = scaling.enabled ? 1 : 2;
        hash = hash * 31 + (scaling.override ? 1 : 0);
        hash = hash * 31 + scaling.grace_radius;
        hash = hash * 31 + scaling.per_blocks;
        hash = hash * 31 + scaling.add_level;
        hash = hash * 31 + scaling.max_add;
        return hash;
    }

    private static int computeHash() {
        int hash = 17;
        Config config = ConfigBridge.config();
        if (config != null && config.dimensions != null) {
            for (Config.Dimension dimension : config.dimensions) {
                if (dimension == null || dimension.zones == null) {
                    continue;
                }
                hash = hash * 31 + scalingHash(
                        ConfigBridge.forDimension(dimension).distance_scaling);
                for (Config.Zone zone : dimension.zones) {
                    hash = hash * 31 + String.valueOf(
                            zone.zone_matches == null ? "" : zone.zone_matches.structure).hashCode();
                    hash = hash * 31 + String.valueOf(
                            zone.zone_matches == null ? "" : zone.zone_matches.biome).hashCode();
                    hash = hash * 31 + (zone.difficulty == null ? 0
                            : String.valueOf(zone.difficulty.name).hashCode() + zone.difficulty.level);
                    hash = hash * 31 + rangeHash(zone);
                }
            }
        }
        for (ZoneInfo zone : zones) {
            hash = hash * 31 + zone.name().hashCode();
            for (ZoneInfo.Resistance resistance : zone.resistances()) {
                hash = hash * 31 + resistance.damageId().hashCode();
                hash = hash * 31 + Float.floatToIntBits(resistance.percent());
                hash = hash * 31 + Float.floatToIntBits(resistance.flat());
                hash = hash * 31 + String.valueOf(resistance.displayKey()).hashCode();
                hash = hash * 31 + String.valueOf(resistance.displayText()).hashCode();
            }
        }
        return hash;
    }
}
