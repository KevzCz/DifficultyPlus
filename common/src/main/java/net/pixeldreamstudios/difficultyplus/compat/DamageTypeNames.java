package net.pixeldreamstudios.difficultyplus.compat;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

public final class DamageTypeNames {
    private DamageTypeNames() {}

    public static MutableComponent of(ResourceLocation id, boolean tag,
                               String displayKey, String displayText) {
        if (displayKey != null && !displayKey.isEmpty()) {
            return Component.translatable(displayKey);
        }
        if (displayText != null && !displayText.isEmpty()) {
            return Component.literal(displayText);
        }
        if (id == null) {
            return Component.empty();
        }
        String key = defaultKey(id);
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        return Component.literal(prettify(id, tag));
    }

    public static String defaultKey(ResourceLocation id) {
        return DifficultyPlus.MOD_ID + ".damagetype." + id.getPath().replace('/', '.');
    }

    private static String prettify(ResourceLocation id, boolean tag) {
        String raw = id.getPath();
        int slash = raw.lastIndexOf('/');
        if (slash >= 0) {
            raw = raw.substring(slash + 1);
        }
        if (tag && raw.startsWith("is_")) {
            raw = raw.substring(3);
        }
        raw = raw.replace('_', ' ');
        if (raw.isEmpty()) {
            return raw;
        }
        return Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
