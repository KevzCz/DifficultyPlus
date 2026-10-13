package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.logic.ItemScaling;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = ItemScaling.class, remap = false)
public class RewardCurveMixin {

    @ModifyArg(method = "applyModifiersForItemStack",
            at = @At(value = "INVOKE",
                    target = "Lnet/dungeon_difficulty/config/Config$AttributeModifier;randomizedValue(I)F"),
            remap = false)
    private static int difficultyplus$curveRewardLevel(int level) {
        return ConfigBridge.global().reward_curve.apply(level);
    }
}
