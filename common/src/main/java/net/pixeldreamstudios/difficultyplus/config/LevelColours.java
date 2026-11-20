package net.pixeldreamstudios.difficultyplus.config;

import java.util.ArrayList;
import java.util.List;

public class LevelColours {

    public static class Stop {

        public int level;

        public String colour;

        private transient String parsedFrom;

        private transient int parsed;

        private transient boolean ready;

        public Stop() {}

        public Stop(int level, String colour) {
            this.level = level;
            this.colour = colour;
        }

        public int rgb() {
            String current = colour;
            if (!ready || current != parsedFrom) {
                parsed = parse(current);
                parsedFrom = current;
                ready = true;
            }
            return parsed;
        }
    }

    public static List<Stop> defaults() {
        List<Stop> out = new ArrayList<>();
        out.add(new Stop(0, "#A9BCC6"));
        out.add(new Stop(10, "#86ADD0"));
        out.add(new Stop(20, "#5F92D0"));
        out.add(new Stop(30, "#4A6FC2"));
        out.add(new Stop(40, "#D6B24C"));
        out.add(new Stop(50, "#DB8B45"));
        out.add(new Stop(60, "#D2643F"));
        out.add(new Stop(70, "#BE3F3F"));
        out.add(new Stop(80, "#A23466"));
        out.add(new Stop(90, "#7E3A9C"));
        out.add(new Stop(100, "#6544A8"));
        out.add(new Stop(110, "#4F3C93"));
        out.add(new Stop(120, "#3F2F7A"));
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
        return (banded == null ? stops.get(0) : banded).rgb();
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
