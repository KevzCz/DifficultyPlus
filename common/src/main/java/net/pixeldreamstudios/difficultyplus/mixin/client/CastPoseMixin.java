package net.pixeldreamstudios.difficultyplus.mixin.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.client.CastPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public class CastPoseMixin {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("RETURN"))
    private void difficultyplus$castPose(LivingEntity entity, float limbSwing,
                                         float limbSwingAmount, float age, float headYaw,
                                         float headPitch, CallbackInfo ci) {
        CastPose.apply((HumanoidModel<?>) (Object) this, entity, age);
    }
}
