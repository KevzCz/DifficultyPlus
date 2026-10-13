package net.pixeldreamstudios.difficultyplus.config;

import java.util.List;

public class GlobalData {

    public int zone_poll_interval = 20;

    public float resistance_reduction_cap = 0.80f;

    public int biome_fill_budget = 2;

    public LevelGapData level_gap = new LevelGapData();

    public RewardCurve reward_curve = new RewardCurve();

    public LateEquipment late_equipment = new LateEquipment();

    public StructureRefresh structure_refresh = new StructureRefresh();

    public ZoneMap zone_map = new ZoneMap();

    public static class LateEquipment {
        public boolean enabled = true;

        public int scale_within_ticks = 100;
    }

    public static class RewardCurve {
        public float scale = 1.0f;
        public Integer ceiling = null;

        public int apply(int level) {
            if (level <= 0) {
                return level;
            }
            int curved = Math.max(1, Math.round(level * Math.max(0f, scale)));
            return ceiling == null ? curved : Math.min(curved, Math.max(0, ceiling));
        }
    }

    public static class ZoneMap {
        public boolean enabled = true;

        public int send_distance = 0;

        public boolean quart_detail = true;
    }

    public static class StructureRefresh {
        public boolean enabled = true;

        public int cooldown_minutes = 60;

        public int chunks_per_tick = 4;

        public boolean clear_owned_entities = true;

        public List<String> never_clear = List.of();

        public List<String> blacklist = List.of(
                "#minecraft:village",
                "minecraft:shipwreck",
                "minecraft:shipwreck_beached");

        public List<String> reroll_blacklist = List.of();

        public float hollow_skull_chance = 0.08f;

        public int hollow_skull_min_level = 1;

        public float sapphire_chance = 0.03f;

        public int sapphire_min_level = 2;

        public float garnet_chance = 0.01f;

        public int garnet_min_level = 3;

        public List<String> key_blocks = List.of(
                "minecraft:chest",
                "minecraft:trapped_chest",
                "minecraft:barrel",
                "minecraft:shulker_box",
                "minecraft:ender_chest",
                "minecraft:beacon",
                "#minecraft:beds",
                "#minecraft:shulker_boxes");

        public int cooldownTicks() {
            return Math.max(0, cooldown_minutes) * 60 * 20;
        }
    }

    public int pollInterval() {
        return Math.max(1, zone_poll_interval);
    }

    public int refreshDuration() {
        return pollInterval() * 2;
    }
}
