package net.pixeldreamstudios.difficultyplus.internal;

public final class LevelGate {
    private LevelGate() {}

    public static boolean allows(int minLevel, int maxLevel, int level) {
        if (minLevel <= 0 && maxLevel <= 0) {
            return true;
        }
        if (level < 0) {
            return true;
        }
        if (minLevel > 0 && level < minLevel) {
            return false;
        }
        return maxLevel <= 0 || level <= maxLevel;
    }
}
