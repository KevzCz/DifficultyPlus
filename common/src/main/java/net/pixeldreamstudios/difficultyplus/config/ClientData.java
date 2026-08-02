package net.pixeldreamstudios.difficultyplus.config;

import java.util.List;

public class ClientData {

    public boolean show_overlay = true;

    public boolean show_resistances = true;

    public boolean persist = true;

    public float opacity = 0.35f;

    public boolean smooth_borders = true;

    public boolean colour_by_level = false;

    public List<LevelColours.Stop> level_colours = LevelColours.defaults();
}
