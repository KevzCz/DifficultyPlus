package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneLabels;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerCommonPacketListenerImpl.class)
public class AnnouncementLabelMixin {

    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> difficultyplus$labelAnnouncement(Packet<?> packet) {
        if (packet instanceof ClientboundSetSubtitleTextPacket subtitle) {
            return new ClientboundSetSubtitleTextPacket(ZoneLabels.labeled(subtitle.text()));
        }
        return packet;
    }
}
