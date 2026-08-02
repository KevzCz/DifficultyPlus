package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.DungeonDifficulty;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.ReloadHandler;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = DungeonDifficulty.class, remap = false)
public class DungeonDifficultyMixin {

    @Inject(
            method = "reloadConfig",
            at = @At(value = "INVOKE", target = "Lnet/dungeon_difficulty/logic/DifficultyTypes;resolve()V"),
            locals = LocalCapture.CAPTURE_FAILHARD,
            remap = false)
    private static void difficultyplus$captureConfig(CallbackInfo ci, Config config) {
        ConfigBridge.capture(config);
    }

    @Inject(method = "reloadConfig", at = @At("RETURN"), remap = false)
    private static void difficultyplus$afterReload(CallbackInfo ci) {
        ReloadHandler.afterConfigReload();
    }
}
