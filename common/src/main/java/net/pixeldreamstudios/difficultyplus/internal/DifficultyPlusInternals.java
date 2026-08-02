package net.pixeldreamstudios.difficultyplus.internal;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.api.LevelResolver;
import net.pixeldreamstudios.difficultyplus.api.ResistanceProvider;
import net.pixeldreamstudios.difficultyplus.api.StructureKey;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;
import net.pixeldreamstudios.difficultyplus.internal.damage.ConfigResistanceProvider;
import net.pixeldreamstudios.difficultyplus.internal.damage.ResistanceEngine;
import net.pixeldreamstudios.difficultyplus.internal.level.LevelStore;
import net.pixeldreamstudios.difficultyplus.internal.level.ScaledZoneAccess;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneResolver;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class DifficultyPlusInternals {
    private DifficultyPlusInternals() {}

    private static final List<LevelResolver> LEVEL_RESOLVERS = new ArrayList<>();
    private static final List<ResistanceProvider> RESISTANCE_PROVIDERS = new ArrayList<>();

    public static void registerLevelResolver(LevelResolver resolver) {
        LEVEL_RESOLVERS.add(resolver);
        LEVEL_RESOLVERS.sort(Comparator.comparingInt(LevelResolver::priority));
    }

    public static List<LevelResolver> levelResolvers() {
        return LEVEL_RESOLVERS;
    }

    public static void registerResistanceProvider(ResistanceProvider provider) {
        RESISTANCE_PROVIDERS.add(provider);
    }

    public static List<ResistanceProvider> resistanceProviders() {
        return RESISTANCE_PROVIDERS;
    }

    public static OptionalInt levelAt(ServerLevel level, BlockPos pos) {
        return ZoneResolver.at(level, pos)
                .map(context -> OptionalInt.of(context.zoneLevel()))
                .orElse(OptionalInt.empty());
    }

    public static OptionalInt levelOf(ServerLevel level, StructureKey key) {
        return LevelStore.get(level).indexed(key);
    }

    public static Optional<ZoneContext> zoneAt(ServerLevel level, BlockPos pos) {
        return ZoneResolver.at(level, pos);
    }

    public static float effectiveResistance(LivingEntity entity, DamageSource source) {
        return ResistanceEngine.totalReduction(entity, source);
    }

    public static Optional<DifficultyPlusAPI.ScaledZone> scaledZoneOf(LivingEntity entity) {
        if (entity == null) {
            return Optional.empty();
        }
        ScaledZoneAccess access = (ScaledZoneAccess) entity;
        String zone = access.difficultyplus$scaledZone();
        if (zone == null) {
            return Optional.empty();
        }
        return Optional.of(new DifficultyPlusAPI.ScaledZone(
                zone,
                access.difficultyplus$scaledZoneLevel(),
                access.difficultyplus$scaledZoneMatch()));
    }

    public static List<DifficultyPlusAPI.ResistanceInfo> resistancesOf(LivingEntity entity) {
        return ConfigResistanceProvider.describe(entity);
    }

    public static List<DifficultyPlusAPI.ResistanceInfo> playerResistancesAt(ServerLevel level,
                                                                            BlockPos pos) {
        return ConfigResistanceProvider.describePlayerAt(level, pos);
    }

    public static Optional<String> zoneNameOf(LivingEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return ZoneResolver.at(level, entity.blockPosition()).map(ZoneContext::difficulty);
    }
}
