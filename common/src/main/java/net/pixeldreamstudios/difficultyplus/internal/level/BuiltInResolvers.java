package net.pixeldreamstudios.difficultyplus.internal.level;

import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.api.LevelResolver;
import net.pixeldreamstudios.difficultyplus.api.StructureContext;
import net.pixeldreamstudios.difficultyplus.internal.zone.LevelRoll;
import net.pixeldreamstudios.difficultyplus.internal.zone.StructureLookup;

import java.util.Optional;
import java.util.OptionalInt;

public final class BuiltInResolvers {
    private BuiltInResolvers() {}

    public static void register() {
        DifficultyPlusAPI.registerLevelResolver(new StoredOverride());
        DifficultyPlusAPI.registerLevelResolver(new HashedRoll());
    }

    private static final class StoredOverride implements LevelResolver {

        @Override
        public OptionalInt resolve(StructureContext context) {
            Optional<StructureStart> start =
                    StructureLookup.startAt(context.level(), context.origin());
            if (start.isEmpty()) {
                return OptionalInt.empty();
            }
            StructureLevelAccess access = (StructureLevelAccess) (Object) start.get();
            int stored = access.difficultyplus$level();
            if (stored == StructureLevelAccess.NONE) {
                return OptionalInt.empty();
            }
            return OptionalInt.of(Math.clamp(stored, context.min(), context.max()));
        }

        @Override
        public int priority() {
            return 100;
        }
    }

    private static final class HashedRoll implements LevelResolver {

        @Override
        public OptionalInt resolve(StructureContext context) {
            long salt = StructureLookup.startAt(context.level(), context.origin())
                    .map(start -> (long) ((StructureLevelAccess) (Object) start).difficultyplus$salt())
                    .orElse(0L);
            long seed = context.level().getSeed() + salt * -7046029254386353131L;
            return OptionalInt.of(
                    LevelRoll.hashedLevel(seed, context.key(), context.min(), context.max()));
        }

        @Override
        public int priority() {
            return 10000;
        }
    }
}
