package net.pixeldreamstudios.difficultyplus.mixin.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.pixeldreamstudios.difficultyplus.client.SocketFlash;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class SocketFlashMixin {

    @Inject(method = "renderSlot", at = @At("RETURN"))
    private void difficultyplus$flash(GuiGraphics graphics, Slot slot, CallbackInfo ci) {
        SocketFlash.render(graphics, slot);
    }
}
