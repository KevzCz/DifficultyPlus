package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.internal.level.LevelGap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class LevelGapDamageMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float difficultyplus$levelGap(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (amount <= 0f) {
            return amount;
        }
        if (self instanceof ServerPlayer victim) {
            if (source.getEntity() instanceof LivingEntity mob && !(mob instanceof ServerPlayer)) {
                return amount * LevelGap.takenMultiplier(victim, mob);
            }
            return amount;
        }
        if (source.getEntity() instanceof ServerPlayer attacker) {
            return amount * LevelGap.dealtMultiplier(attacker, self);
        }
        if (source.getEntity() instanceof LivingEntity mob) {
            return amount * LevelGap.mobVsMobMultiplier(mob, self);
        }
        return amount;
    }
}
