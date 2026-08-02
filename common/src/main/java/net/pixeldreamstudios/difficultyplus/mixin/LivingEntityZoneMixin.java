package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.internal.level.ScaledZoneAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityZoneMixin implements ScaledZoneAccess {

    @Unique
    private String difficultyplus$zone;
    @Unique
    private int difficultyplus$zoneLevel = ScaledZoneAccess.NO_LEVEL;
    @Unique
    private String difficultyplus$zoneMatch;

    @Override
    public String difficultyplus$scaledZone() {
        return difficultyplus$zone;
    }

    @Override
    public void difficultyplus$setScaledZone(String zone) {
        this.difficultyplus$zone = zone;
    }

    @Override
    public int difficultyplus$scaledZoneLevel() {
        return difficultyplus$zoneLevel;
    }

    @Override
    public void difficultyplus$setScaledZoneLevel(int level) {
        this.difficultyplus$zoneLevel = level;
    }

    @Override
    public String difficultyplus$scaledZoneMatch() {
        return difficultyplus$zoneMatch;
    }

    @Override
    public void difficultyplus$setScaledZoneMatch(String match) {
        this.difficultyplus$zoneMatch = match;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void difficultyplus$write(CompoundTag tag, CallbackInfo ci) {
        if (difficultyplus$zone == null) {
            return;
        }
        tag.putString("difficultyplus_zone", difficultyplus$zone);
        tag.putInt("difficultyplus_zone_level", difficultyplus$zoneLevel);
        if (difficultyplus$zoneMatch != null) {
            tag.putString("difficultyplus_zone_match", difficultyplus$zoneMatch);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void difficultyplus$read(CompoundTag tag, CallbackInfo ci) {
        if (!tag.contains("difficultyplus_zone")) {
            return;
        }
        difficultyplus$zone = tag.getString("difficultyplus_zone");
        difficultyplus$zoneLevel = tag.contains("difficultyplus_zone_level")
                ? tag.getInt("difficultyplus_zone_level")
                : ScaledZoneAccess.NO_LEVEL;
        difficultyplus$zoneMatch = tag.contains("difficultyplus_zone_match")
                ? tag.getString("difficultyplus_zone_match")
                : null;
    }
}
