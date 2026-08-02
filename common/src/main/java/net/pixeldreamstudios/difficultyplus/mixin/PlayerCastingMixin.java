package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.difficultyplus.internal.structure.CastingAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerCastingMixin implements CastingAccess {

    @Unique
    private static final EntityDataAccessor<Boolean> DIFFICULTYPLUS$CASTING =
            SynchedEntityData.defineId(Player.class, EntityDataSerializers.BOOLEAN);

    @Inject(method = "defineSynchedData", at = @At("RETURN"))
    private void difficultyplus$define(SynchedEntityData.Builder builder, CallbackInfo ci) {
        builder.define(DIFFICULTYPLUS$CASTING, false);
    }

    @Override
    public boolean difficultyplus$casting() {
        return ((Player) (Object) this).getEntityData().get(DIFFICULTYPLUS$CASTING);
    }

    @Override
    public void difficultyplus$setCasting(boolean casting) {
        ((Player) (Object) this).getEntityData().set(DIFFICULTYPLUS$CASTING, casting);
    }
}
