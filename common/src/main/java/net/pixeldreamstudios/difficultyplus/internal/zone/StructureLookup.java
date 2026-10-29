package net.pixeldreamstudios.difficultyplus.internal.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.api.StructureKey;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;

public final class StructureLookup {
    private static final ThreadLocal<Memo> LAST = new ThreadLocal<>();

    private StructureLookup() {}

    private record Memo(WeakReference<ServerLevel> level, long chunk, long time, List<StructureStart> starts) {}

    private static List<StructureStart> startsAt(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        long time = level.getGameTime();
        Memo memo = LAST.get();
        if (memo != null && memo.level().get() == level && memo.chunk() == chunk.toLong() && memo.time() == time) {
            return memo.starts();
        }
        List<StructureStart> starts = level.structureManager().startsForStructure(chunk, s -> true);
        LAST.set(new Memo(new WeakReference<>(level), chunk.toLong(), time, starts));
        return starts;
    }

    public static Optional<StructureKey> keyAt(ServerLevel level, BlockPos pos, ResourceLocation matchedId) {
        if (matchedId == null) {
            return Optional.empty();
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : startsAt(level, pos)) {
            if (!start.isValid()) {
                continue;
            }
            ResourceLocation id = registry.getKey(start.getStructure());
            if (id == null || !id.equals(matchedId)) {
                continue;
            }
            BoundingBox box = start.getBoundingBox();
            if (box.isInside(pos)) {
                return Optional.of(StructureKey.of(id, box.getCenter()));
            }
        }
        return Optional.empty();
    }

    public static Optional<StructureKey> enclosing(ServerLevel level, BlockPos pos) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : startsAt(level, pos)) {
            if (!start.isValid()) {
                continue;
            }
            BoundingBox box = start.getBoundingBox();
            if (!box.isInside(pos)) {
                continue;
            }
            ResourceLocation id = registry.getKey(start.getStructure());
            if (id != null) {
                return Optional.of(StructureKey.of(id, box.getCenter()));
            }
        }
        return Optional.empty();
    }

    public static Optional<StructureStart> startAt(ServerLevel level, BlockPos pos) {
        for (StructureStart start : startsAt(level, pos)) {
            if (start.isValid() && start.getBoundingBox().isInside(pos)) {
                return Optional.of(start);
            }
        }
        return Optional.empty();
    }
}
