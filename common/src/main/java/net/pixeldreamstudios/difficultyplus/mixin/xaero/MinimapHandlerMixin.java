package net.pixeldreamstudios.difficultyplus.mixin.xaero;

import net.pixeldreamstudios.difficultyplus.compat.xaero.MinimapRefresh;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.highlight.DimensionHighlighterHandler;

@Mixin(value = DimensionHighlighterHandler.class, remap = false)
public class MinimapHandlerMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void difficultyplus$track(CallbackInfo ci) {
        MinimapRefresh.track((DimensionHighlighterHandler) (Object) this);
    }
}
