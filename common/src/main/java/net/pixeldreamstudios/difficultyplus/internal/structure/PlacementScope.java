package net.pixeldreamstudios.difficultyplus.internal.structure;

public final class PlacementScope {
    private PlacementScope() {}

    private static final ThreadLocal<Integer> DEPTH = new ThreadLocal<>();

    public static void push() {
        Integer current = DEPTH.get();
        DEPTH.set(current == null ? 1 : current + 1);
    }

    public static void pop() {
        Integer current = DEPTH.get();
        if (current == null || current <= 1) {
            DEPTH.remove();
            return;
        }
        DEPTH.set(current - 1);
    }

    public static boolean active() {
        return DEPTH.get() != null;
    }
}
