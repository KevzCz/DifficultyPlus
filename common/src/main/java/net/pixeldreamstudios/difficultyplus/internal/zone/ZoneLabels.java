package net.pixeldreamstudios.difficultyplus.internal.zone;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.DifficultyTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;

import java.util.Locale;

public final class ZoneLabels {
    private ZoneLabels() {}

    private static final String TYPE_KEY_PREFIX = "difficulty.type.";
    private static final String SEPARATOR = " · ";

    public static String labelOf(String difficultyName) {
        String label = ConfigBridge.forDifficulty(difficultyName).label;
        return label == null || label.isBlank() ? null : label;
    }

    public static MutableComponent text(String label) {
        return Component.translatableWithFallback(label, label);
    }

    public static Component labeled(Component subtitle) {
        if (subtitle == null || !(subtitle.getContents() instanceof TranslatableContents contents)) {
            return subtitle;
        }
        String key = contents.getKey();
        if (!key.startsWith(TYPE_KEY_PREFIX)) {
            return subtitle;
        }
        String label = labelOf(typeForCode(key.substring(TYPE_KEY_PREFIX.length())));
        if (label == null) {
            return subtitle;
        }
        return text(label).withStyle(ChatFormatting.GREEN)
                .append(Component.literal(SEPARATOR).withStyle(ChatFormatting.GRAY))
                .append(subtitle);
    }

    private static String typeForCode(String code) {
        for (Config.DifficultyType type : DifficultyTypes.resolved) {
            String own = type.translation_code != null ? type.translation_code : type.name;
            if (own != null && own.toLowerCase(Locale.ENGLISH).equals(code)) {
                return type.name;
            }
        }
        return null;
    }
}
