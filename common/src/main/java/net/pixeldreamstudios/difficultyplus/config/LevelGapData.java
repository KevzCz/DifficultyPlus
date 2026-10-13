package net.pixeldreamstudios.difficultyplus.config;

import java.util.List;

public class LevelGapData {

    public boolean enabled = false;

    public List<String> categories = List.of();

    public float player_scale = 1.0f;

    public Mode mode = Mode.SUM;

    public List<CategoryWeight> weights = List.of();

    public enum Mode {
        SUM,
        HIGHEST,
        AVERAGE
    }

    public static class CategoryWeight {

        public String category = "";

        public float weight = 1.0f;
    }

    public float weightOf(String category) {
        if (weights == null) {
            return 1.0f;
        }
        for (CategoryWeight entry : weights) {
            if (entry != null && category.equals(entry.category)) {
                return Math.max(0f, entry.weight);
            }
        }
        return 1.0f;
    }

    public Mode modeOrDefault() {
        return mode == null ? Mode.SUM : mode;
    }

    public Half player_below = new Half(true, 0.15f, 0.10f);

    public Half player_above = new Half(false, 0.10f, 0.05f);

    public MobVsMob mob_vs_mob = new MobVsMob();

    public static class MobVsMob {

        public boolean enabled = false;

        public Half attacker_below = new Half(true, 0.15f, 0f);

        public Half attacker_above = new Half(true, 0.10f, 0f);
    }

    public static class Half {

        public boolean enabled;

        public float dealt_per_level;

        public float taken_per_level;

        public float min_multiplier = 0.25f;

        public float max_multiplier = 2.0f;

        public Half() {
            this(false, 0f, 0f);
        }

        public Half(boolean enabled, float dealtPerLevel, float takenPerLevel) {
            this.enabled = enabled;
            this.dealt_per_level = dealtPerLevel;
            this.taken_per_level = takenPerLevel;
        }

        public boolean isActive() {
            return enabled && (dealt_per_level != 0f || taken_per_level != 0f);
        }

        public float clamp(float multiplier) {
            float low = min_multiplier <= 0f ? 0.05f : min_multiplier;
            float high = max_multiplier <= low ? low : max_multiplier;
            return Math.max(low, Math.min(high, multiplier));
        }
    }

}
