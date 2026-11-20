package net.pixeldreamstudios.difficultyplus.config;

import java.util.List;

public class ClientData {

    public enum OverlayStyle { OUTLINE, WASH, FULL }

    public enum ColourMode { LEVEL, ZONE, RELATIVE }

    public boolean show_overlay = false;

    public float overlay_opacity = 1.0f;

    public boolean minimap_overlay = true;

    public boolean world_map_overlay = true;

    public boolean show_tooltips = true;

    public boolean show_resistances = true;

    public boolean persist = true;

    public OverlayStyle minimap_style = OverlayStyle.OUTLINE;

    public OverlayStyle world_map_style = OverlayStyle.WASH;

    public float opacity = 0.35f;

    public float wash_opacity = 0.12f;

    public float border_opacity = 0.85f;

    public float step_opacity = 0.55f;

    public boolean smooth_borders = true;

    public int smoothing = 1;

    public boolean colour_by_level = false;

    public ColourMode colour_mode;

    public int relative_safe_gap = 3;

    public int relative_warn_gap = 10;

    public int relative_danger_gap = 25;

    public int relative_cleared_gap = 10;

    public List<LevelColours.Stop> level_colours = LevelColours.defaults();

    public ColourMode colourMode() {
        return colour_mode != null ? colour_mode : colour_by_level ? ColourMode.LEVEL : ColourMode.ZONE;
    }

    public OverlayStyle minimapStyle() {
        return minimap_style == null ? OverlayStyle.OUTLINE : minimap_style;
    }

    public OverlayStyle worldMapStyle() {
        return world_map_style == null ? OverlayStyle.WASH : world_map_style;
    }
}
