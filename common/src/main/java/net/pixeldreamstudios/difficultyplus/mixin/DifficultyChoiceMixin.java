package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.logic.PatternMatching;
import net.dungeon_difficulty.logic.ScalingGoal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.pixeldreamstudios.difficultyplus.internal.zone.ResolvingLevel;
import net.pixeldreamstudios.difficultyplus.internal.zone.RolledLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternMatching.class, remap = false)
public class DifficultyChoiceMixin {

    @Inject(method = "getDifficultyResult", at = @At("HEAD"), remap = false)
    private static void difficultyplus$openScope(PatternMatching.LocationData location,
                                                 ResourceLocation entityId,
                                                 ScalingGoal goal,
                                                 ServerLevel level,
                                                 CallbackInfoReturnable<
                                                         PatternMatching.DifficultySearchResult> cir) {
        ResolvingLevel.push(level);
    }

    @Inject(method = "getDifficultyResult", at = @At("RETURN"), remap = false)
    private static void difficultyplus$closeScope(PatternMatching.LocationData location,
                                                  ResourceLocation entityId,
                                                  ScalingGoal goal,
                                                  ServerLevel level,
                                                  CallbackInfoReturnable<
                                                          PatternMatching.DifficultySearchResult> cir) {
        ResolvingLevel.pop();
    }

    @Inject(method = "chooseHigherDifficulty", at = @At("HEAD"), cancellable = true, remap = false)
    private static void difficultyplus$compareRolled(
            PatternMatching.DifficultySearchResult zone,
            PatternMatching.DifficultySearchResult entity,
            CallbackInfoReturnable<PatternMatching.DifficultySearchResult> cir) {

        ServerLevel level = ResolvingLevel.current();
        if (level == null || zone == null || entity == null) {
            return;
        }

        int zoneLevel = RolledLevel.preview(level, zone);
        int entityLevel = RolledLevel.preview(level, entity);
        if (zoneLevel == Integer.MIN_VALUE && entityLevel == Integer.MIN_VALUE) {
            return;
        }
        cir.setReturnValue(zoneLevel >= entityLevel ? zone : entity);
    }
}
