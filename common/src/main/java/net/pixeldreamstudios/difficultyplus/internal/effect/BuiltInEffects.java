package net.pixeldreamstudios.difficultyplus.internal.effect;

import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;

public final class BuiltInEffects {
    private BuiltInEffects() {}

    public static void register() {
        register("attribute", AttributeEffect::fromJson);
        register("status_effect", StatusEffectZoneEffect::fromJson);
        register("damage_over_time", DamageOverTimeEffect::fromJson);
    }

    private static void register(String path, DifficultyPlusAPI.ZoneEffectRegistry.Factory factory) {
        DifficultyPlusAPI.zoneEffects()
                .register(ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, path), factory);
    }
}
