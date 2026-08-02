package net.pixeldreamstudios.difficultyplus.internal.damage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.api.ResistanceProvider;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;
import net.pixeldreamstudios.difficultyplus.config.DifficultyPlusData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.DifficultyPlusInternals;
import net.pixeldreamstudios.difficultyplus.internal.LevelGate;
import net.pixeldreamstudios.difficultyplus.internal.effect.TargetMatcher;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ConfigResistanceProvider implements ResistanceProvider {

    @Override
    public Result resistance(LivingEntity target, DamageSource source) {
        if (!(target.level() instanceof ServerLevel level)) {
            return Result.NONE;
        }

        String liveZone = ZoneResolver.at(level, target.blockPosition())
                .map(ZoneContext::difficulty).orElse(null);
        String frozenZone = target instanceof Player
                ? liveZone
                : DifficultyPlusInternals.scaledZoneOf(target)
                        .map(DifficultyPlusAPI.ScaledZone::difficulty).orElse(liveZone);

        if (liveZone == null && frozenZone == null) {
            return Result.NONE;
        }

        float flat = 0f;
        float remaining = 1f;

        for (DifficultyPlusData.ResistanceEntry.Scope scope
                : DifficultyPlusData.ResistanceEntry.Scope.values()) {
            String zoneName = scope == DifficultyPlusData.ResistanceEntry.Scope.FROZEN
                    ? frozenZone : liveZone;
            if (zoneName == null) {
                continue;
            }
            DifficultyPlusData data = ConfigBridge.forDifficulty(zoneName);

            List<DifficultyPlusData.ResistanceEntry> entries =
                    target instanceof Player ? data.player_resistances : data.mob_resistances;
            if (entries == null || entries.isEmpty()) {
                continue;
            }
            for (DifficultyPlusData.ResistanceEntry entry : entries) {
                if (entry.scope != scope
                        || !matches(entry, source)
                        || !TargetMatcher.matchesResistance(entry.targets, target)) {
                    continue;
                }
                flat += entry.flat;
                remaining *= (1f - entry.percent);
            }
        }
        return new Result(flat, 1f - remaining);
    }

    private static boolean matches(DifficultyPlusData.ResistanceEntry entry, DamageSource source) {
        if (entry.damage_tag != null && !entry.damage_tag.isEmpty()) {
            String raw = entry.damage_tag.startsWith("#") ? entry.damage_tag.substring(1) : entry.damage_tag;
            ResourceLocation id = ResourceLocation.tryParse(raw);
            return id != null && source.is(TagKey.create(Registries.DAMAGE_TYPE, id));
        }
        if (entry.damage_type != null && !entry.damage_type.isEmpty()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.damage_type);
            return id != null && source.is(ResourceKey.create(Registries.DAMAGE_TYPE, id));
        }
        return false;
    }

    public static List<DifficultyPlusAPI.ResistanceInfo> describe(LivingEntity target) {
        if (target == null || !(target.level() instanceof ServerLevel level)) {
            return List.of();
        }

        Optional<ZoneContext> live = ZoneResolver.at(level, target.blockPosition());
        String liveZone = live.map(ZoneContext::difficulty).orElse(null);
        int liveLevel = live.map(ZoneContext::zoneLevel).orElse(-1);

        Optional<DifficultyPlusAPI.ScaledZone> scaled = target instanceof Player
                ? Optional.empty()
                : DifficultyPlusInternals.scaledZoneOf(target);
        String frozenZone = scaled.map(DifficultyPlusAPI.ScaledZone::difficulty).orElse(liveZone);
        int frozenLevel = scaled.map(DifficultyPlusAPI.ScaledZone::level).orElse(liveLevel);

        List<DifficultyPlusData.ResistanceEntry> entries = new ArrayList<>();
        for (DifficultyPlusData.ResistanceEntry.Scope scope
                : DifficultyPlusData.ResistanceEntry.Scope.values()) {
            boolean frozen = scope == DifficultyPlusData.ResistanceEntry.Scope.FROZEN;
            String zoneName = frozen ? frozenZone : liveZone;
            int zoneLevel = frozen ? frozenLevel : liveLevel;
            if (zoneName == null) {
                continue;
            }
            DifficultyPlusData data = ConfigBridge.forDifficulty(zoneName);
            List<DifficultyPlusData.ResistanceEntry> list =
                    target instanceof Player ? data.player_resistances : data.mob_resistances;
            if (list == null) {
                continue;
            }
            for (DifficultyPlusData.ResistanceEntry entry : list) {
                if (entry.scope == scope
                        && LevelGate.allows(entry.min_level, entry.max_level, zoneLevel)) {
                    entries.add(entry);
                }
            }
        }
        if (entries.isEmpty()) {
            return List.of();
        }

        List<DifficultyPlusAPI.ResistanceInfo> out = new ArrayList<>();
        for (DifficultyPlusData.ResistanceEntry entry : entries) {
            if (!TargetMatcher.matchesResistance(entry.targets, target)) {
                continue;
            }

            DifficultyPlusAPI.ResistanceInfo info = toInfo(entry);
            if (info != null) {
                out.add(info);
            }
        }
        return List.copyOf(out);
    }

    public static List<DifficultyPlusAPI.ResistanceInfo> describePlayerAt(ServerLevel level,
                                                                          BlockPos pos) {
        return describePlayerAt(level, pos, null);
    }

    public static List<DifficultyPlusAPI.ResistanceInfo> describePlayerAt(ServerLevel level,
                                                                          BlockPos pos,
                                                                          Player player) {
        if (level == null || pos == null) {
            return List.of();
        }
        Optional<ZoneContext> zone = ZoneResolver.at(level, pos);
        String zoneName = zone.map(ZoneContext::difficulty).orElse(null);
        return zoneName == null ? List.of()
                : describeZone(zoneName, player, zone.map(ZoneContext::zoneLevel).orElse(-1));
    }

    public static List<DifficultyPlusAPI.ResistanceInfo> describeZone(String zoneName,
                                                                      Player player) {
        return describeZone(zoneName, player, -1);
    }

    public static List<DifficultyPlusAPI.ResistanceInfo> describeZone(String zoneName,
                                                                      Player player,
                                                                      int zoneLevel) {
        if (zoneName == null) {
            return List.of();
        }
        DifficultyPlusData data = ConfigBridge.forDifficulty(zoneName);
        if (data.player_resistances == null) {
            return List.of();
        }

        List<DifficultyPlusAPI.ResistanceInfo> out = new ArrayList<>();
        for (DifficultyPlusData.ResistanceEntry entry : data.player_resistances) {
            if (player != null && !TargetMatcher.matchesResistance(entry.targets, player)) {
                continue;
            }
            if (!LevelGate.allows(entry.min_level, entry.max_level, zoneLevel)) {
                continue;
            }
            DifficultyPlusAPI.ResistanceInfo info = toInfo(entry);
            if (info != null) {
                out.add(info);
            }
        }
        return List.copyOf(out);
    }

    private static DifficultyPlusAPI.ResistanceInfo toInfo(DifficultyPlusData.ResistanceEntry entry) {
        if (entry.percent == 0f && entry.flat == 0f) {
            return null;
        }
        boolean isTag = entry.damage_tag != null && !entry.damage_tag.isEmpty();
        String raw = isTag ? entry.damage_tag : entry.damage_type;
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            return null;
        }
        return new DifficultyPlusAPI.ResistanceInfo(id, isTag, entry.percent, entry.flat,
                entry.display_key, entry.display_text, entry.min_level, entry.max_level);
    }

    public static boolean isResolvable(DifficultyPlusData.ResistanceEntry entry) {
        if (entry.damage_tag != null && !entry.damage_tag.isEmpty()) {
            String raw = entry.damage_tag.startsWith("#") ? entry.damage_tag.substring(1) : entry.damage_tag;
            return ResourceLocation.tryParse(raw) != null;
        }
        if (entry.damage_type != null && !entry.damage_type.isEmpty()) {
            return ResourceLocation.tryParse(entry.damage_type) != null;
        }
        return false;
    }
}
