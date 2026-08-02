package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.internal.level.LateGear;
import net.pixeldreamstudios.difficultyplus.internal.level.LateGearAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LateGearMixin implements LateGearAccess {

    @Unique
    private boolean difficultyplus$gearScaled;

    @Override
    public boolean difficultyplus$gearScaled() {
        return difficultyplus$gearScaled;
    }

    @Override
    public void difficultyplus$setGearScaled(boolean scaled) {
        this.difficultyplus$gearScaled = scaled;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void difficultyplus$scaleLateGear(CallbackInfo ci) {
        LateGear.tick((LivingEntity) (Object) this);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void difficultyplus$write(CompoundTag tag, CallbackInfo ci) {
        if (difficultyplus$gearScaled) {
            tag.putBoolean("difficultyplus_gear_scaled", true);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void difficultyplus$read(CompoundTag tag, CallbackInfo ci) {
        difficultyplus$gearScaled = tag.getBoolean("difficultyplus_gear_scaled");
    }
}
