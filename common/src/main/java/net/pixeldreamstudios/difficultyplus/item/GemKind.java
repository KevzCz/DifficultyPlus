package net.pixeldreamstudios.difficultyplus.item;

public enum GemKind {

    SAPPHIRE(0xFF4A8FD4, false),

    GARNET(0xFFC43D3D, true);

    public static final int SURVEY_COLOUR = 0xFFFFFFFF;

    public static final int REFUSED_COLOUR = 0xFF6E6A7A;

    public static final int BLOCKED_COLOUR = 0xFFFF3020;

    private final int glowColour;

    private final boolean rerolls;

    GemKind(int glowColour, boolean rerolls) {
        this.glowColour = glowColour;
        this.rerolls = rerolls;
    }

    public int glowColour() {
        return glowColour;
    }

    public boolean rerolls() {
        return rerolls;
    }
}
