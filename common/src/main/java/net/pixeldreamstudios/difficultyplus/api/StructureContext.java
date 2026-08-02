package net.pixeldreamstudios.difficultyplus.api;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

public record StructureContext(ServerLevel level,
                               StructureKey key,
                               BlockPos origin,
                               String difficulty,
                               int configured,
                               int min,
                               int max) {

    public boolean isRanged() {
        return max > min;
    }

    public ResourceLocation structureId() {
        return key.structureId();
    }
}
