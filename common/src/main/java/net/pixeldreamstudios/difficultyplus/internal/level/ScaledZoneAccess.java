package net.pixeldreamstudios.difficultyplus.internal.level;

public interface ScaledZoneAccess {
    int NO_LEVEL = Integer.MIN_VALUE;

    String difficultyplus$scaledZone();

    void difficultyplus$setScaledZone(String zone);

    int difficultyplus$scaledZoneLevel();

    void difficultyplus$setScaledZoneLevel(int level);

    String difficultyplus$scaledZoneMatch();

    void difficultyplus$setScaledZoneMatch(String match);
}
