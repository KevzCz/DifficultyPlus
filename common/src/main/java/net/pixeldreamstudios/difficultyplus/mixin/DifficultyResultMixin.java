package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.logic.Difficulty;
import net.dungeon_difficulty.logic.PatternMatching;
import net.dungeon_difficulty.logic.ScalingGoal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.pixeldreamstudios.difficultyplus.internal.zone.RolledLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatternMatching.class, remap = false)
public class DifficultyResultMixin {

    @Inject(method = "getDifficultyResult", at = @At("RETURN"), cancellable = true, remap = false)
    private static void difficultyplus$applyRolledLevel(
            PatternMatching.LocationData location,
            ResourceLocation entityId,
            ScalingGoal goal,
            ServerLevel level,
            CallbackInfoReturnable<PatternMatching.DifficultySearchResult> cir) {

        PatternMatching.DifficultySearchResult result = cir.getReturnValue();
        if (result == null || level == null) {
            return;
        }

        Difficulty difficulty = result.difficulty();
        if (difficulty == null || !difficulty.isValid()) {
            return;
        }

        Difficulty rolled = RolledLevel.apply(level, location, result, difficulty);
        if (rolled != difficulty) {
            cir.setReturnValue(new PatternMatching.DifficultySearchResult(
                    rolled, result.locationData(), result.match()));
        }
    }
}
