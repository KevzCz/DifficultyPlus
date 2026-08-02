package net.pixeldreamstudios.difficultyplus.mixin.xaero;

import net.pixeldreamstudios.difficultyplus.compat.xaero.XaeroBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.highlight.HighlighterRegistry;

@Mixin(value = HighlighterRegistry.class, remap = false)
public class WorldMapRegistryMixin {

    @Inject(method = "end", at = @At("HEAD"))
    private void difficultyplus$addZoneHighlighter(CallbackInfo ci) {
        ((HighlighterRegistry) (Object) this).register(XaeroBridge.worldMapHighlighter());
    }
}
