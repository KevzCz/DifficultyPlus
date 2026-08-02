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
        out.add(new Stop(1, "#4C9A52"));
        out.add(new Stop(10, "#C9B23A"));
        out.add(new Stop(20, "#D4813C"));
        out.add(new Stop(31, "#A53A2F"));
        return out;
    }

    public static int rgbAt(List<Stop> stops, int level) {
        if (stops == null || stops.isEmpty()) {
            return 0xFFFFFF;
        }
        Stop below = null;
        Stop above = null;
        for (Stop stop : stops) {
            if (stop.level <= level && (below == null || stop.level > below.level)) {
                below = stop;
            }
            if (stop.level >= level && (above == null || stop.level < above.level)) {
                above = stop;
            }
        }
        if (below == null) {
            return parse(above.colour);
        }
        if (above == null || above.level == below.level) {
            return parse(below.colour);
        }
        float t = (level - below.level) / (float) (above.level - below.level);
        return blend(parse(below.colour), parse(above.colour), t);
    }

    private static int blend(int from, int to, float t) {
        int red = channel(from, 16, to, t);
        int green = channel(from, 8, to, t);
        int blue = channel(from, 0, to, t);
        return (red << 16) | (green << 8) | blue;
    }

    private static int channel(int from, int shift, int to, float t) {
        int a = (from >> shift) & 0xFF;
        int b = (to >> shift) & 0xFF;
        int value = (int) (a + (b - a) * t);
        return Math.max(0, Math.min(255, value));
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
