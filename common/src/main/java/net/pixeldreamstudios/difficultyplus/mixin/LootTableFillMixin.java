package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.pixeldreamstudios.difficultyplus.item.ReliquaryLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LootTable.class)
public class LootTableFillMixin {

    @Inject(method = "fill", at = @At("RETURN"))
    private void difficultyplus$injectReliquary(Container container, LootParams params, long seed,
                                                CallbackInfo ci) {
        ReliquaryLoot.injectInto(container, params, seed);
    }
}
