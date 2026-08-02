package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.internal.structure.PlacementScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureStart.class)
public class StructurePlacementMixin {

    @Inject(method = "placeInChunk", at = @At("HEAD"))
    private void difficultyplus$open(WorldGenLevel level, StructureManager structureManager,
                                     ChunkGenerator generator, RandomSource random,
                                     BoundingBox box, ChunkPos chunkPos, CallbackInfo ci) {
        PlacementScope.push();
    }

    @Inject(method = "placeInChunk", at = @At("RETURN"))
    private void difficultyplus$close(WorldGenLevel level, StructureManager structureManager,
                                      ChunkGenerator generator, RandomSource random,
                                      BoundingBox box, ChunkPos chunkPos, CallbackInfo ci) {
        PlacementScope.pop();
    }
}
