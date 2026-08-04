package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.pixeldreamstudios.difficultyplus.internal.structure.LootOriginAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class LootOriginMixin implements LootOriginAccess {

    @Unique
    private ResourceKey<LootTable> difficultyplus$origin;

    @Override
    public ResourceKey<LootTable> difficultyplus$originalLootTable() {
        return difficultyplus$origin;
    }

    @Override
    public void difficultyplus$setOriginalLootTable(ResourceKey<LootTable> table) {
        this.difficultyplus$origin = table;
    }

    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceKey;)V", at = @At("HEAD"))
    private void difficultyplus$rememberLootTable(ResourceKey<LootTable> table, CallbackInfo ci) {
        if (table != null) {
            difficultyplus$origin = table;
        }
    }
}
