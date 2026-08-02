package net.pixeldreamstudios.difficultyplus.api;

import net.minecraft.server.level.ServerPlayer;
import java.util.OptionalInt;

public interface LevelResolver {

    OptionalInt resolve(StructureContext context);

    default int priority() {
        return 1000;
    }

    interface PlayerAware extends LevelResolver {
        OptionalInt resolve(StructureContext context, ServerPlayer player);
    }
}
