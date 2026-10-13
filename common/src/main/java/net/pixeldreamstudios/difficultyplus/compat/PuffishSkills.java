package net.pixeldreamstudios.difficultyplus.compat;

import dev.architectury.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.puffish.skillsmod.api.Category;
import net.puffish.skillsmod.api.SkillsAPI;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.config.LevelGapData;

import java.util.List;
import java.util.OptionalInt;
import java.util.function.ToDoubleFunction;

public final class PuffishSkills {
    private PuffishSkills() {}

    private static final String MOD_ID = "puffish_skills";

    private static Boolean present;

    public static boolean isLoaded() {
        if (present == null) {
            present = Platform.isModLoaded(MOD_ID);
        }
        return present;
    }

    public static OptionalInt totalLevel(ServerPlayer player, List<String> categories) {
        return combinedLevel(player, categories, LevelGapData.Mode.SUM, id -> 1.0f);
    }

    public static OptionalInt combinedLevel(ServerPlayer player, List<String> categories,
                                            LevelGapData.Mode mode, ToDoubleFunction<String> weightOf) {
        if (player == null || !isLoaded()) {
            return OptionalInt.empty();
        }
        try {
            double total = 0;
            double highest = 0;
            double weightSum = 0;
            boolean any = false;

            for (Category category : SkillsAPI.streamCategories().toList()) {
                if (!matches(category, categories) || !category.isUnlocked(player)) {
                    continue;
                }
                Integer level = category.getExperience()
                        .map(experience -> experience.getLevel(player))
                        .orElse(null);
                if (level == null) {
                    continue;
                }
                double weight = weightOf.applyAsDouble(category.getId().toString());
                if (weight <= 0) {
                    continue;
                }
                total += level * weight;
                highest = Math.max(highest, level * weight);
                weightSum += weight;
                any = true;
            }
            if (!any) {
                return OptionalInt.empty();
            }
            double combined = switch (mode == null ? LevelGapData.Mode.SUM : mode) {
                case SUM -> total;
                case HIGHEST -> highest;
                case AVERAGE -> total / weightSum;
            };
            return OptionalInt.of((int) Math.round(combined));
        } catch (Throwable t) {
            present = false;
            DifficultyPlus.LOGGER.warn("Difficulty+ could not read Puffish Skills levels", t);
            return OptionalInt.empty();
        }
    }

    public static List<String> categoryIds() {
        if (!isLoaded()) {
            return List.of();
        }
        try {
            return SkillsAPI.streamCategories()
                    .map(Category::getId)
                    .map(ResourceLocation::toString)
                    .toList();
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static boolean matches(Category category, List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return true;
        }
        String id = category.getId().toString();
        return categories.contains(id);
    }
}
