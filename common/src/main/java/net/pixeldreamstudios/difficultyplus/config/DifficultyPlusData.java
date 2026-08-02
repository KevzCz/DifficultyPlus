package net.pixeldreamstudios.difficultyplus.config;

import com.google.gson.JsonObject;
import java.util.List;

public class DifficultyPlusData {

    public List<ResistanceEntry> mob_resistances = List.of();

    public List<ResistanceEntry> player_resistances = List.of();

    public List<ZoneEffectEntry> zone_effects = List.of();

    public static class ResistanceEntry {

        public String damage_type;

        public String damage_tag;

        public String display_key;

        public String display_text;

        public TargetFilter targets;

        public float percent = 0f;

        public float flat = 0f;

        public int min_level = 0;

        public int max_level = 0;

        public Scope scope = Scope.FROZEN;

        public enum Scope {

            FROZEN,

            LIVE
        }
    }

    public static class ZoneEffectEntry {

        public String type;

        public TargetFilter targets;

        public int duration = 0;

        public int interval = 0;

        public int min_level = 0;

        public int max_level = 0;

        public boolean linger = false;

        public String attribute;

        public String operation;

        public Double value;

        public String effect;

        public Integer amplifier;

        public String damage_type;

        public Float amount;

        public transient JsonObject raw;
    }

    public static class TargetFilter {

        public String attitude = "ANY";

        public String type = "";
    }
}
