package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.world.entity.Entity;
import net.pixeldreamstudios.difficultyplus.internal.structure.StructureOutline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MarkerSaveMixin {

    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void difficultyplus$skipMarkers(CallbackInfoReturnable<Boolean> cir) {
        if (((Entity) (Object) this).getTags().contains(StructureOutline.TRANSIENT_TAG)) {
            cir.setReturnValue(false);
        }
    }
}
