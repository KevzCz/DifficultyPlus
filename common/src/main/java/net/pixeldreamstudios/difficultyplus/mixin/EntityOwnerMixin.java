package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.pixeldreamstudios.difficultyplus.internal.structure.StructureOwnerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityOwnerMixin implements StructureOwnerAccess {

    @Unique
    private String difficultyplus$structureOwner;

    @Override
    public String difficultyplus$structureOwner() {
        return difficultyplus$structureOwner;
    }

    @Override
    public void difficultyplus$setStructureOwner(String owner) {
        this.difficultyplus$structureOwner = owner;
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"))
    private void difficultyplus$write(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (difficultyplus$structureOwner != null) {
            tag.putString("difficultyplus_structure_owner", difficultyplus$structureOwner);
        }
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void difficultyplus$read(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("difficultyplus_structure_owner")) {
            difficultyplus$structureOwner = tag.getString("difficultyplus_structure_owner");
        }
    }
}
