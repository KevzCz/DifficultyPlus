package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import net.pixeldreamstudios.difficultyplus.internal.spawn.SpawnRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
    private static void difficultyplus$spawnRules(ServerLevel level, Mob mob, double distance, CallbackInfoReturnable<Boolean> info) {
        if (!SpawnRules.allows(level, mob)) {
            info.setReturnValue(false);
        }
    }
}
