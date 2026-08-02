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

import java.util.Optional;

public final class StructureLookup {
    private StructureLookup() {}

    public static Optional<StructureKey> keyAt(ServerLevel level, BlockPos pos, ResourceLocation matchedId) {
        if (matchedId == null) {
            return Optional.empty();
        }
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : level.structureManager()
                .startsForStructure(new ChunkPos(pos), s -> true)) {
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
        for (StructureStart start : level.structureManager()
                .startsForStructure(new ChunkPos(pos), s -> true)) {
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
        for (StructureStart start : level.structureManager()
                .startsForStructure(new ChunkPos(pos), s -> true)) {
            if (start.isValid() && start.getBoundingBox().isInside(pos)) {
                return Optional.of(start);
            }
        }
        return Optional.empty();
    }
}
