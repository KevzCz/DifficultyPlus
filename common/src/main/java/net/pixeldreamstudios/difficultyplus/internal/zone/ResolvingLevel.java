package net.pixeldreamstudios.difficultyplus.internal.zone;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public final class ResolvingLevel {
    private ResolvingLevel() {}

    private static final ThreadLocal<List<ServerLevel>> STACK =
            ThreadLocal.withInitial(ArrayList::new);

    public static void push(ServerLevel level) {
        STACK.get().add(level);
    }

    public static void pop() {
        List<ServerLevel> stack = STACK.get();
        if (!stack.isEmpty()) {
            stack.remove(stack.size() - 1);
        }
        if (stack.isEmpty()) {
            STACK.remove();
        }
    }

    public static ServerLevel current() {
        List<ServerLevel> stack = STACK.get();
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }
}
