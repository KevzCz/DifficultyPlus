package net.pixeldreamstudios.difficultyplus.internal.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.api.StructureKey;
import net.pixeldreamstudios.difficultyplus.api.ZoneContext;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.internal.level.LevelStore;
import net.pixeldreamstudios.difficultyplus.internal.level.StructureLevelAccess;
import net.pixeldreamstudios.difficultyplus.internal.zone.StructureLookup;
import net.pixeldreamstudios.difficultyplus.internal.zone.ZoneResolver;
import java.util.Optional;

public final class LevelCommands {
    private LevelCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("difficultyplus")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("level")
                        .then(Commands.literal("query").executes(LevelCommands::query))
                        .then(Commands.literal("set")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                        .executes(LevelCommands::set)))
                        .then(Commands.literal("reroll").executes(LevelCommands::reroll))
                        .then(Commands.literal("clear").executes(LevelCommands::clear)))
                .then(Commands.literal("why")
                        .executes(context -> DiagnosticsCommand.why(context.getSource())))
                .then(Commands.literal("structure")
                        .then(Commands.literal("refresh")
                                .executes(context ->
                                        DiagnosticsCommand.refresh(context.getSource(), false)))
                        .then(Commands.literal("reroll")
                                .executes(context ->
                                        DiagnosticsCommand.refresh(context.getSource(), true)))
                        .then(Commands.literal("cooldown")
                                .then(Commands.literal("clear")
                                        .executes(context ->
                                                DiagnosticsCommand.clearCooldown(
                                                        context.getSource())))))
                .then(Commands.literal("debug").executes(LevelCommands::toggleDebug));
        dispatcher.register(root);
    }

    private static int toggleDebug(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("Only a player can subscribe to debug output."));
            return 0;
        }

        boolean enabled = !DifficultyPlus.isDebugEnabled();
        DifficultyPlus.setDebug(player.getUUID(), enabled);
        source.sendSuccess(() -> Component.literal(
                "Difficulty+ debug " + (enabled ? "on" : "off")), false);
        return 1;
    }

    private static int query(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        DifficultyPlus.debug(() -> String.format(
                "query @ %d,%d,%d  biome=%s", pos.getX(), pos.getY(), pos.getZ(),
                level.getBiome(pos).unwrapKey()
                        .map(k -> k.location().toString()).orElse("?")));

        Optional<ZoneContext> zone = ZoneResolver.at(level, pos);
        if (zone.isEmpty()) {
            source.sendFailure(Component.literal("No Difficulty+ zone here."));
            return 0;
        }
        ZoneContext ctx = zone.get();
        Optional<StructureStart> start = StructureLookup.startAt(level, pos);
        String origin = start
                .map(s -> ((StructureLevelAccess) (Object) s).difficultyplus$source())
                .orElse(null);

        source.sendSuccess(() -> Component.literal(
                "Zone: " + ctx.difficulty()
                        + " | level " + ctx.zoneLevel()
                        + " | source " + (origin == null ? "hash" : origin)
                        + ctx.structureId().map(id -> " | " + id).orElse("")), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> context) {
        int value = IntegerArgumentType.getInteger(context, "level");
        return write(context.getSource(), value, StructureLevelAccess.SOURCE_SET);
    }

    private static int reroll(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        return withStructure(source, (level, pos, start, key) -> {
            StructureLevelAccess access = (StructureLevelAccess) (Object) start;

            access.difficultyplus$setSalt(access.difficultyplus$salt() + 1);
            access.difficultyplus$setLevel(StructureLevelAccess.NONE);
            access.difficultyplus$setSource(StructureLevelAccess.SOURCE_REROLL);
            invalidate(level, key);
            source.sendSuccess(() -> Component.literal("Rerolled " + key.structureId()), true);
            return 1;
        });
    }

    private static int clear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        return withStructure(source, (level, pos, start, key) -> {
            StructureLevelAccess access = (StructureLevelAccess) (Object) start;
            access.difficultyplus$setLevel(StructureLevelAccess.NONE);
            access.difficultyplus$setSalt(0);
            access.difficultyplus$setSource(null);
            LevelStore.get(level).forget(key);
            invalidate(level, key);
            source.sendSuccess(() -> Component.literal("Cleared override on " + key.structureId()), true);
            return 1;
        });
    }

    private static int write(CommandSourceStack source, int value, String origin) {
        return withStructure(source, (level, pos, start, key) -> {
            StructureLevelAccess access = (StructureLevelAccess) (Object) start;
            access.difficultyplus$setLevel(value);
            access.difficultyplus$setSource(origin);
            LevelStore.get(level).index(key, value);
            invalidate(level, key);
            source.sendSuccess(() -> Component.literal(
                    "Set " + key.structureId() + " to level " + value
                            + " (affects newly spawned mobs)"), true);
            return 1;
        });
    }

    private static int withStructure(CommandSourceStack source, StructureAction action) {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());
        Optional<StructureStart> start = StructureLookup.startAt(level, pos);
        if (start.isEmpty()) {
            source.sendFailure(Component.literal("Not inside a structure."));
            return 0;
        }
        var registry = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE);
        var id = registry.getKey(start.get().getStructure());
        if (id == null) {
            source.sendFailure(Component.literal("Unregistered structure."));
            return 0;
        }
        StructureKey key = StructureKey.of(id, start.get().getBoundingBox().getCenter());
        return action.run(level, pos, start.get(), key);
    }

    private static void invalidate(ServerLevel level, StructureKey key) {
        BlockPos center = key.centerPos();
        level.getChunk(center).setUnsaved(true);
        ZoneResolver.invalidate();
    }

    @FunctionalInterface
    private interface StructureAction {
        int run(ServerLevel level, BlockPos pos, StructureStart start, StructureKey key);
    }
}
