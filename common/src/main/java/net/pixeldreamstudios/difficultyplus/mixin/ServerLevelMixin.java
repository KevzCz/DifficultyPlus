package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.server.level.ServerLevel;
import net.pixeldreamstudios.difficultyplus.internal.effect.ZoneEffectManager;
import net.pixeldreamstudios.difficultyplus.internal.structure.BlockHighlight;
import net.pixeldreamstudios.difficultyplus.internal.structure.CastRitual;
import net.pixeldreamstudios.difficultyplus.internal.structure.RefreshQueue;
import net.pixeldreamstudios.difficultyplus.internal.structure.SurveyTracker;
import net.pixeldreamstudios.difficultyplus.internal.zone.BiomePatch;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneChunkSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void difficultyplus$tickZones(CallbackInfo ci) {
        BiomePatch.resetTickBudget();
        ZoneEffectManager.tick((ServerLevel) (Object) this);
        BlockHighlight.tick((ServerLevel) (Object) this);
        SurveyTracker.tick((ServerLevel) (Object) this);
        CastRitual.tickCasts((ServerLevel) (Object) this);
        CastRitual.tickFlights((ServerLevel) (Object) this);
        RefreshQueue.tick((ServerLevel) (Object) this);
        ZoneChunkSync.flush((ServerLevel) (Object) this);
    }
}
