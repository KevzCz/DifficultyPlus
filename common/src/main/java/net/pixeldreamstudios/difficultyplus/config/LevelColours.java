package net.pixeldreamstudios.difficultyplus.config;

import java.util.ArrayList;
import java.util.List;

public class LevelColours {

    public static class Stop {

        public int level;

        public String colour;

        public Stop() {}

        public Stop(int level, String colour) {
            this.level = level;
            this.colour = colour;
        }
    }

    public static List<Stop> defaults() {
        List<Stop> out = new ArrayList<>();
        out.add(new Stop(1, "#2ECC71"));
        out.add(new Stop(6, "#3498DB"));
        out.add(new Stop(11, "#F1C40F"));
        out.add(new Stop(16, "#E67E22"));
        out.add(new Stop(21, "#E74C3C"));
        out.add(new Stop(26, "#9B59B6"));
        out.add(new Stop(31, "#ECF0F1"));
        return out;
    }

    public static int rgbAt(List<Stop> stops, int level) {
        if (stops == null || stops.isEmpty()) {
            return 0xFFFFFF;
        }
        Stop banded = null;
        for (Stop stop : stops) {
            if (stop.level <= level && (banded == null || stop.level > banded.level)) {
                banded = stop;
            }
        }
        return parse((banded == null ? stops.get(0) : banded).colour);
    }

    public static int parse(String colour) {
        if (colour == null || colour.isEmpty()) {
            return 0xFFFFFF;
        }
        String raw = colour.startsWith("#") ? colour.substring(1) : colour;
        try {
            return (int) (Long.parseLong(raw, 16) & 0xFFFFFF);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
