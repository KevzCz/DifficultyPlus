package net.pixeldreamstudios.difficultyplus.config;

public class ZoneData {

    public LevelRange level_range = new LevelRange();

    public DistanceScaling distance_scaling = new DistanceScaling();

    public static class LevelRange {

        public enum Mode {

            RELATIVE,

            ABSOLUTE
        }

        public Mode mode = Mode.RELATIVE;

        public int min = 0;

        public int max = 0;

        public Integer floor = null;
        public Integer ceiling = null;

        public int region_size = 512;

        public boolean biome_fill = false;

        public int biome_fill_cap = 1024;

        public boolean lock_on_generation = false;

        public Double bias_scale = null;

        public int scaledBias(int bias, int low, int high) {
            if (bias == 0 || bias_scale == null || bias_scale <= 0) {
                return bias;
            }
            int scaled = (int) Math.round(bias * bias_scale * Math.max(1, high - low));
            return scaled == 0 ? Integer.signum(bias) : scaled;
        }

        public boolean isValid() {
            return max >= min && (mode != Mode.ABSOLUTE || min >= 0);
        }

        public boolean isRanged() {
            return isValid() && max > min;
        }

        public int lowestFor(int zoneLevel) {
            return lowestFor(zoneLevel, false, false);
        }

        public int highestFor(int zoneLevel) {
            return highestFor(zoneLevel, false, false);
        }

        public int lowestFor(int zoneLevel, boolean pierceFloor, boolean pierceCeiling) {
            return clamp(mode == Mode.ABSOLUTE ? min : zoneLevel + min,
                    pierceFloor, pierceCeiling);
        }

        public int highestFor(int zoneLevel, boolean pierceFloor, boolean pierceCeiling) {
            return Math.max(lowestFor(zoneLevel, pierceFloor, pierceCeiling),
                    clamp(mode == Mode.ABSOLUTE ? max : zoneLevel + max,
                            pierceFloor, pierceCeiling));
        }

        private int clamp(int level, boolean pierceFloor, boolean pierceCeiling) {
            int result = Math.max(0, level);
            if (floor != null && !pierceFloor) {
                result = Math.max(result, floor);
            }

            if (ceiling != null && !pierceCeiling
                    && (floor == null || pierceFloor || ceiling >= floor)) {
                result = Math.min(result, ceiling);
            }
            return result;
        }
    }
}
