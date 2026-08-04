package net.pixeldreamstudios.difficultyplus.api;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.pixeldreamstudios.difficultyplus.internal.DifficultyPlusInternals;
import net.pixeldreamstudios.difficultyplus.internal.level.LevelGap;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

public final class DifficultyPlusAPI {
    private DifficultyPlusAPI() {}

    private static final ZoneEffectRegistry ZONE_EFFECTS = new ZoneEffectRegistry();

    public static ZoneEffectRegistry zoneEffects() {
        return ZONE_EFFECTS;
    }

    public static void registerLevelResolver(LevelResolver resolver) {
        DifficultyPlusInternals.registerLevelResolver(resolver);
    }

    public static void registerResistanceProvider(ResistanceProvider provider) {
        DifficultyPlusInternals.registerResistanceProvider(provider);
    }

    public static OptionalInt levelAt(ServerLevel level, BlockPos pos) {
        return DifficultyPlusInternals.levelAt(level, pos);
    }

    public static OptionalInt levelOf(ServerLevel level, StructureKey key) {
        return DifficultyPlusInternals.levelOf(level, key);
    }

    public static Optional<ZoneContext> zoneAt(ServerLevel level, BlockPos pos) {
        return DifficultyPlusInternals.zoneAt(level, pos);
    }

    public static Optional<ScaledZone> scaledZoneOf(LivingEntity entity) {
        return DifficultyPlusInternals.scaledZoneOf(entity);
    }

    public record ScaledZone(String difficulty, int level, String matchId) {}

    public static float effectiveResistance(LivingEntity entity, DamageSource source) {
        return DifficultyPlusInternals.effectiveResistance(entity, source);
    }

    public static List<ResistanceInfo> resistancesOf(LivingEntity entity) {
        return DifficultyPlusInternals.resistancesOf(entity);
    }

    public static List<ResistanceInfo> playerResistancesAt(ServerLevel level, BlockPos pos) {
        return DifficultyPlusInternals.playerResistancesAt(level, pos);
    }

    public static OptionalInt playerLevel(ServerPlayer player) {
        return LevelGap.playerLevel(player);
    }

    public static OptionalInt levelGap(ServerPlayer viewer, LivingEntity mob) {
        return LevelGap.gap(viewer, mob);
    }

    public static boolean levelGapActive() {
        return LevelGap.active();
    }

    public static <T> Optional<T> bandOf(ServerPlayer viewer, LivingEntity mob,
                                         List<GapBand<T>> bands) {
        OptionalInt gap = LevelGap.gap(viewer, mob);
        return gap.isEmpty() ? Optional.empty() : bandFor(gap.getAsInt(), bands);
    }

    public static <T> Optional<T> bandFor(int gap, List<GapBand<T>> bands) {
        if (bands == null || bands.isEmpty()) {
            return Optional.empty();
        }
        T matched = null;
        int best = Integer.MIN_VALUE;
        for (GapBand<T> band : bands) {
            if (band == null || gap < band.minGap()) {
                continue;
            }
            if (band.minGap() >= best) {
                best = band.minGap();
                matched = band.value();
            }
        }
        return Optional.ofNullable(matched);
    }

    public record GapBand<T>(int minGap, T value) {}

    public static Optional<String> zoneNameOf(LivingEntity entity) {
        return DifficultyPlusInternals.zoneNameOf(entity);
    }

    public record ResistanceInfo(ResourceLocation damageId, boolean isTag, float percent, float flat,
                                 String displayKey, String displayText,
                                 int minLevel, int maxLevel) {

        public ResistanceInfo(ResourceLocation damageId, boolean isTag, float percent, float flat) {
            this(damageId, isTag, percent, flat, null, null, 0, 0);
        }

        public ResistanceInfo(ResourceLocation damageId, boolean isTag, float percent, float flat,
                              String displayKey, String displayText) {
            this(damageId, isTag, percent, flat, displayKey, displayText, 0, 0);
        }

        public boolean isVulnerability() {
            return percent < 0f || flat < 0f;
        }
    }

    public static final class ZoneEffectRegistry {
        private final Map<ResourceLocation, Factory> factories = new LinkedHashMap<>();

        public void register(ResourceLocation id, Factory factory) {
            factories.put(id, factory);
        }

        public Factory get(ResourceLocation id) {
            return factories.get(id);
        }

        public boolean has(ResourceLocation id) {
            return factories.containsKey(id);
        }

        public List<ResourceLocation> ids() {
            return List.copyOf(factories.keySet());
        }

        @FunctionalInterface
        public interface Factory {
            ZoneEffect create(JsonObject json);
        }
    }
}
