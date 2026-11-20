package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.logic.PatternMatching;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.internal.zone.StructureContainment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternMatching.class, remap = false)
public class StructurePiecesMixin {

    @Inject(method = "isInsideStructure", at = @At("HEAD"), cancellable = true, remap = false)
    private static void difficultyplus$insidePieces(ServerLevel level, BlockPos pos, StructureStart start,
                                                    CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(StructureContainment.contains(start, pos));
    }
}
