package net.pixeldreamstudios.difficultyplus.mixin;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.DifficultyTypes;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigAccess;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = DifficultyTypes.class, remap = false)
public class DifficultyTypesMixin {

    @Inject(method = "copy", at = @At("RETURN"), remap = false)
    private static void difficultyplus$carryData(Config.DifficultyType source,
                                                 CallbackInfoReturnable<Config.DifficultyType> cir) {
        Config.DifficultyType copy = cir.getReturnValue();
        if (copy == null || source == null) {
            return;
        }
        ((ConfigAccess.Type) (Object) copy)
                .difficultyplus$setData(((ConfigAccess.Type) (Object) source).difficultyplus$data());
    }

    @Inject(method = "merge", at = @At("RETURN"), remap = false)
    private static void difficultyplus$mergeData(Config.DifficultyType child,
                                                 Config.DifficultyType parent,
                                                 CallbackInfoReturnable<Config.DifficultyType> cir) {
        Config.DifficultyType merged = cir.getReturnValue();
        if (merged == null) {
            return;
        }
        DifficultyPlusData childData = ((ConfigAccess.Type) (Object) child).difficultyplus$data();
        DifficultyPlusData parentData = ((ConfigAccess.Type) (Object) parent).difficultyplus$data();

        DifficultyPlusData result = new DifficultyPlusData();
        result.mob_resistances = concat(childData.mob_resistances, parentData.mob_resistances);
        result.player_resistances = concat(childData.player_resistances, parentData.player_resistances);
        result.zone_effects = concat(childData.zone_effects, parentData.zone_effects);
        ((ConfigAccess.Type) (Object) merged).difficultyplus$setData(result);
    }

    private static <T> List<T> concat(List<T> child, List<T> parent) {
        if (child == null || child.isEmpty()) {
            return parent == null ? List.of() : parent;
        }
        if (parent == null || parent.isEmpty()) {
            return child;
        }
        List<T> combined = new ArrayList<>(child.size() + parent.size());
        combined.addAll(child);
        combined.addAll(parent);
        return List.copyOf(combined);
    }
}
