package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.internal.damage.ResistanceEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = LivingEntity.class, priority = 1100)
public abstract class LivingEntityDamageMixin {

    @ModifyVariable(method = "hurt", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float difficultyplus$resist(float amount, DamageSource source) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.isInvulnerableTo(source) || self.isDeadOrDying()) {
            return amount;
        }
        return ResistanceEngine.apply(self, source, amount);
    }
}
