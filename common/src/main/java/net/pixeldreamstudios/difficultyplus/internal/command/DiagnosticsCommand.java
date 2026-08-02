package net.pixeldreamstudios.difficultyplus.internal.command;

import net.dungeon_difficulty.config.Config;
import net.dungeon_difficulty.logic.PatternMatching;
import net.dungeon_difficulty.logic.ScalingGoal;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.api.StructureKey;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.config.DistanceScaling;
import net.pixeldreamstudios.difficultyplus.config.ZoneData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.level.LevelStore;
import net.pixeldreamstudios.difficultyplus.internal.level.StructureLevelAccess;
import net.pixeldreamstudios.difficultyplus.internal.structure.BlockHighlight;
import net.pixeldreamstudios.difficultyplus.internal.structure.PlacementTracker;
import net.pixeldreamstudios.difficultyplus.internal.structure.StructureRefresh;
import net.pixeldreamstudios.difficultyplus.internal.zone.LevelRoll;
import net.pixeldreamstudios.difficultyplus.internal.zone.StructureLookup;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class DiagnosticsCommand {
    private DiagnosticsCommand() {}

    public static int why(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        line(source, "Difficulty+ at " + pos.toShortString(), ChatFormatting.GOLD);

        String biome = level.getBiome(pos).unwrapKey()
                .map(key -> key.location().toString()).orElse("unknown");
        detail(source, "biome", biome);
        detail(source, "dimension", level.dimension().location().toString());

        Optional<StructureStart> start = StructureLookup.startAt(level, pos);
        if (start.isEmpty()) {
            detail(source, "structure", "none at this position");
        } else {
            describeStructure(source, level, start.get());
        }

        describeMatch(source, level, pos);
        describeRange(source, level, pos);
        return 1;
    }

    private static void describeStructure(CommandSourceStack source, ServerLevel level,
                                          StructureStart start) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceLocation id = registry.getKey(start.getStructure());
        BoundingBox box = start.getBoundingBox();
        detail(source, "structure", id == null ? "unregistered" : id.toString());
        detail(source, "  bounds", box.minX() + "," + box.minY() + "," + box.minZ()
                + " to " + box.maxX() + "," + box.maxY() + "," + box.maxZ());
        detail(source, "  centre", box.getCenter().toShortString());

        StructureLevelAccess access = (StructureLevelAccess) (Object) start;
        int stored = access.difficultyplus$level();
        detail(source, "  stored level",
                stored == StructureLevelAccess.NONE ? "none (rolls from hash)" : String.valueOf(stored));
        detail(source, "  salt", String.valueOf(access.difficultyplus$salt()));
        detail(source, "  source",
                access.difficultyplus$source() == null ? "hash" : access.difficultyplus$source());

        if (id != null) {
            StructureKey key = StructureKey.of(id, box.getCenter());
            OptionalInt indexed = LevelStore.get(level).indexed(key);
            detail(source, "  indexed",
                    indexed.isPresent() ? String.valueOf(indexed.getAsInt()) : "not indexed");
        }

        detail(source, "  placements", String.valueOf(PlacementTracker.placementsOf(start).length));
        describeRefresh(source, level, start);
    }

    private static void describeRefresh(CommandSourceStack source, ServerLevel level,
                                        StructureStart start) {
        StructureRefresh.Result result = StructureRefresh.inspect(level, start);
        String text = switch (result.state()) {
            case READY -> "ready";
            case DISABLED -> "disabled in config";
            case EXCLUDED -> "blacklisted";
            case COOLING -> "cooling down, " + result.secondsLeft() + "s left";
            case BLOCKED -> result.keyBlocks().size() + " key block(s) in the way";
            case DONE -> "done";
        };
        detail(source, "  refresh", text);
        for (BlockPos blocked : result.keyBlocks()) {
            detail(source, "    key block", blocked.toShortString());
        }
    }

    private static void describeMatch(CommandSourceStack source, ServerLevel level, BlockPos pos) {
        PatternMatching.LocationData location = PatternMatching.LocationData.create(level, pos);
        PatternMatching.DifficultySearchResult result =
                PatternMatching.getDifficultyResult(location, null, ScalingGoal.ENTITY, level);

        if (result == null || result.difficulty() == null || !result.difficulty().isValid()) {
            detail(source, "match", "no zone matched — mobs here are unscaled");
            return;
        }
        detail(source, "matched zone", result.difficulty().type().name);
        detail(source, "  matched by",
                result.matchId() == null ? "dimension default" : result.matchId().toString());
        detail(source, "  level", String.valueOf(result.difficulty().level()));
        detail(source, "  entity level", String.valueOf(result.difficulty().entityLevel()));
        detail(source, "  reward level", String.valueOf(result.difficulty().rewardLevel()));

        Boolean loot = result.difficulty().type().allow_loot_scaling;
        detail(source, "  loot scaling",
                loot == null ? "false (unset — inherits nothing)" : String.valueOf(loot));
    }

    private static void describeRange(CommandSourceStack source, ServerLevel level, BlockPos pos) {
        PatternMatching.LocationData location = PatternMatching.LocationData.create(level, pos);
        PatternMatching.DifficultySearchResult result =
                PatternMatching.getDifficultyResult(location, null, ScalingGoal.ENTITY, level);
        if (result == null || result.difficulty() == null || !result.difficulty().isValid()) {
            return;
        }

        ResourceLocation matched = result.matchId();
        Config config = ConfigBridge.config();
        if (config == null || config.dimensions == null) {
            return;
        }

        ResourceLocation dimensionId = level.dimension().location();
        for (Config.Dimension dimension : config.dimensions) {
            if (dimension.world_matches != null
                    && !LevelRoll.matchesDimension(dimensionId, dimension.world_matches.dimension)) {
                continue;
            }
            if (dimension.zones == null) {
                continue;
            }
            int index = 0;
            for (Config.Zone zone : dimension.zones) {
                index++;
                if (!LevelRoll.zoneMentions(level, pos, zone, matched)) {
                    continue;
                }
                ZoneData data = ConfigBridge.forZone(zone);
                int base = zone.difficulty == null
                        ? result.difficulty().level() : zone.difficulty.level;
                describeWindow(source, index, zone, data, base,
                        LevelRoll.distanceBonus(level, pos, matched));
                return;
            }
        }
        detail(source, "range", "no zone entry with a level_range matched");
    }

    private static void describeScaling(CommandSourceStack source, ZoneData data) {
        DistanceScaling scaling = data.distance_scaling;
        if (scaling == null || !(scaling.enabled || scaling.override)) {
            detail(source, "  distance_scaling", "inherits dimension");
            return;
        }
        if (!scaling.enabled) {
            detail(source, "  distance_scaling", "off (zone override)");
            return;
        }
        detail(source, "  distance_scaling", "+" + scaling.add_level + " per "
                + scaling.per_blocks + " blocks past " + scaling.grace_radius
                + (scaling.max_add > 0 ? ", max +" + scaling.max_add : ", uncapped"));
    }

    private static void describeWindow(CommandSourceStack source, int index, Config.Zone zone,
                                       ZoneData data, int configuredLevel, int distance) {
        String label = zone.zone_matches == null ? "?"
                : zone.zone_matches.structure != null && !zone.zone_matches.structure.isEmpty()
                        ? zone.zone_matches.structure
                        : String.valueOf(zone.zone_matches.biome);
        detail(source, "range from", "zone #" + index + " (" + label + ")");

        ZoneData.LevelRange range = data.level_range;
        if (range == null || !range.isRanged()) {
            detail(source, "  window", distance == 0
                    ? "not ranged — configured level used as-is"
                    : "not ranged — " + configuredLevel + " + " + distance
                            + " = " + Math.max(0, configuredLevel + distance));
            describeScaling(source, data);
            return;
        }
        detail(source, "  mode", String.valueOf(range.mode));
        int low = Math.max(0, range.lowestFor(configuredLevel) + distance);
        int high = Math.max(low, range.highestFor(configuredLevel) + distance);
        detail(source, "  window", distance == 0
                ? low + ".." + high
                : low + ".." + high + " (includes +" + distance + " distance)");
        detail(source, "  region_size", String.valueOf(range.region_size));
        detail(source, "  lock_on_generation", String.valueOf(range.lock_on_generation));
        describeScaling(source, data);
    }

    public static int clearCooldown(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Optional<StructureStart> start = StructureLookup.startAt(level, pos);
        if (start.isEmpty()) {
            source.sendFailure(Component.literal("Not inside a structure."));
            return 0;
        }
        if (!StructureRefresh.clearCooldown(start.get())) {
            source.sendSuccess(() -> Component.literal("No cooldown was set."), false);
            return 0;
        }
        level.getChunk(start.get().getBoundingBox().getCenter()).setUnsaved(true);
        source.sendSuccess(() -> Component.literal("Refresh cooldown cleared."), true);
        return 1;
    }

    public static int refresh(CommandSourceStack source, boolean reroll) {
        try {
            return runRefresh(source, reroll);
        } catch (Throwable t) {
            DifficultyPlus.LOGGER.error("Difficulty+ refresh failed", t);
            source.sendFailure(Component.literal("Refresh failed: " + t));
            return 0;
        }
    }

    private static int runRefresh(CommandSourceStack source, boolean reroll) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Optional<StructureStart> start = StructureLookup.startAt(level, pos);
        if (start.isEmpty()) {
            source.sendFailure(Component.literal("Not inside a structure."));
            return 0;
        }

        if (reroll && !StructureRefresh.canReroll(level, start.get())) {
            source.sendFailure(Component.literal(
                    "This zone has no level range to re-roll."));
            return 0;
        }

        StructureRefresh.Result result = StructureRefresh.apply(
                level, start.get(), reroll, null, source.getPosition(),
                (chunks, blocks, entities) -> source.sendSuccess(() -> Component.literal(
                        (reroll ? "Rerolled " : "Refreshed ") + chunks + " chunk(s): removed "
                                + blocks + " player block(s), cleared "
                                + entities + " owned entity(ies)."), true));

        switch (result.state()) {
            case DONE -> { }
            case DISABLED -> source.sendFailure(
                    Component.literal("Structure refresh is disabled in the config."));
            case EXCLUDED -> source.sendFailure(
                    Component.literal("This structure is on the refresh blacklist."));
            case COOLING -> source.sendFailure(Component.literal(
                    "On cooldown for another " + result.secondsLeft()
                            + "s. Use /difficultyplus structure cooldown clear to reset it."));
            case BLOCKED -> {
                highlight(source, level, result.keyBlocks());
                source.sendFailure(Component.literal(
                        "Refused: " + result.keyBlocks().size()
                                + " player-placed key block(s) inside, now outlined in red."
                                + " Remove them first."));
                for (BlockPos blocked : result.keyBlocks()) {
                    source.sendFailure(Component.literal("  " + blocked.toShortString()));
                }
            }
            default -> source.sendFailure(Component.literal("Nothing to do."));
        }
        return result.state() == StructureRefresh.Result.State.DONE ? 1 : 0;
    }

    private static void highlight(CommandSourceStack source, ServerLevel level,
                                  List<BlockPos> positions) {
        ServerPlayer player = source.getPlayer();
        if (player == null || positions.isEmpty()) {
            return;
        }
        try {
            BlockHighlight.outline(level, player, positions);
        } catch (Throwable t) {
            DifficultyPlus.LOGGER.error("Difficulty+ highlight failed", t);
        }
    }

    private static void line(CommandSourceStack source, String text, ChatFormatting colour) {
        source.sendSuccess(() -> Component.literal(text).withStyle(colour), false);
    }

    private static void detail(CommandSourceStack source, String key, String value) {
        source.sendSuccess(() -> Component.literal(key + ": ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE)), false);
    }
}
