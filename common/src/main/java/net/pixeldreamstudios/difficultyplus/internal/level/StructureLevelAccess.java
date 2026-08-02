package net.pixeldreamstudios.difficultyplus.internal.level;

public interface StructureLevelAccess {
    int NONE = Integer.MIN_VALUE;
    String SOURCE_LOCKED = "locked";
    String SOURCE_SET = "set";
    String SOURCE_REROLL = "reroll";

    int difficultyplus$level();

    void difficultyplus$setLevel(int level);

    int difficultyplus$salt();

    void difficultyplus$setSalt(int salt);

    String difficultyplus$source();

    void difficultyplus$setSource(String source);

    long difficultyplus$refreshedAt();

    void difficultyplus$setRefreshedAt(long gameTime);

    long[] difficultyplus$placements();

    void difficultyplus$setPlacements(long[] positions);

    int difficultyplus$bias();

    void difficultyplus$setBias(int bias);

    int difficultyplus$pierce();

    void difficultyplus$setPierce(int flags);
}
