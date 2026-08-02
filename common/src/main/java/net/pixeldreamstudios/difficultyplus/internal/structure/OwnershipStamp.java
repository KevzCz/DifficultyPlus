package net.pixeldreamstudios.difficultyplus.internal.structure;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.api.StructureKey;

import java.util.Optional;

public final class OwnershipStamp {
    private OwnershipStamp() {}

    public static void register() {
        EntityEvent.ADD.register(OwnershipStamp::onAdd);
    }

    private static EventResult onAdd(Entity entity, Level level) {
        if (entity instanceof Player || !(level instanceof ServerLevel serverLevel)) {
            return EventResult.pass();
        }
        if (!PlacementScope.active()) {
            return EventResult.pass();
        }
        StructureOwnerAccess access = (StructureOwnerAccess) entity;
        if (access.difficultyplus$structureOwner() != null) {
            return EventResult.pass();
        }
        enclosing(serverLevel, entity).ifPresent(key -> {
            access.difficultyplus$setStructureOwner(key.encode());
            RefreshQueue.exempt(entity);
        });
        return EventResult.pass();
    }

    public static Optional<StructureKey> enclosing(ServerLevel level, Entity entity) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (StructureStart start : level.structureManager()
                .startsForStructure(new ChunkPos(entity.blockPosition()), s -> true)) {
            if (!start.isValid()) {
                continue;
            }
            BoundingBox box = start.getBoundingBox();
            if (!box.isInside(entity.blockPosition())) {
                continue;
            }
            ResourceLocation id = registry.getKey(start.getStructure());
            if (id != null) {
                return Optional.of(StructureKey.of(id, box.getCenter()));
            }
        }
        return Optional.empty();
    }

    public static boolean ownedBy(Entity entity, StructureKey key) {
        return ownedBy(entity, key.encode());
    }

    public static boolean ownedBy(Entity entity, String encodedKey) {
        String owner = ((StructureOwnerAccess) entity).difficultyplus$structureOwner();
        return owner != null && owner.equals(encodedKey);
    }
}
