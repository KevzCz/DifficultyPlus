package net.pixeldreamstudios.difficultyplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
import net.pixeldreamstudios.difficultyplus.config.ClientData;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientDisconnectMixin {

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    private void difficultyplus$saveZones(Screen screen, boolean keepResourcePacks,
                                          CallbackInfo ci) {
        ClientData config = ClientConfig.get();
        if (config.persist) {
            ZoneChunkCache.save();
        }
        ZoneChunkCache.clear();
    }
}
