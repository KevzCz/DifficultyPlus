package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.logic.EntityScaling;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.internal.level.ScalingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityScaling.class, remap = false)
public class EntityScalingMixin {

    @Inject(method = "scale", at = @At("HEAD"), remap = false)
    private static void difficultyplus$push(Entity entity, ServerLevel level, CallbackInfo ci) {
        if (entity instanceof LivingEntity living) {
            ScalingEntity.push(living);
        }
    }

    @Inject(method = "scale", at = @At("RETURN"), remap = false)
    private static void difficultyplus$pop(Entity entity, ServerLevel level, CallbackInfo ci) {
        ScalingEntity.pop();
    }
}
