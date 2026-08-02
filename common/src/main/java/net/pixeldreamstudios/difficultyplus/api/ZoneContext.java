package net.pixeldreamstudios.difficultyplus.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import java.util.Optional;

public record ZoneContext(ServerLevel level,
                          String difficulty,
                          int zoneLevel,
                          Optional<ResourceLocation> structureId,
                          ResourceLocation sourceId) {
}
