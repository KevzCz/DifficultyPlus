package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneChunkSync;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public class ChunkSendMixin {

    @Inject(method = "markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;"
            + "Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("RETURN"))
    private static void difficultyplus$sendZone(ServerPlayer player, LevelChunk chunk,
                                                CallbackInfo ci) {
        ZoneChunkSync.onChunkSent(player, chunk.getPos());
    }
}
