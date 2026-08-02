package net.pixeldreamstudios.difficultyplus.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.client.CastPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandLayer.class)
public class HideHeldItemMixin {

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void difficultyplus$hideWhileCasting(PoseStack pose, MultiBufferSource buffers,
                                                 int light, LivingEntity entity, float limbSwing,
                                                 float limbSwingAmount, float partialTick,
                                                 float age, float headYaw, float headPitch,
                                                 CallbackInfo ci) {
        if (CastPose.isCasting(entity)) {
            ci.cancel();
        }
    }
}
