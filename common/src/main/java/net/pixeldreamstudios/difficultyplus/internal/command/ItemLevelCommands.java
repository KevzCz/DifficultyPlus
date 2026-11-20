package net.pixeldreamstudios.difficultyplus.internal.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import java.util.List;

public final class ItemLevelCommands {

    private static final String KEY = "command.difficultyplus.item.";
    private static final String ARG_LEVEL = "level";
    private static final String ARG_TARGETS = "targets";
    private static final int MIN_LEVEL = 1;
    private static final int NO_LEVEL = 0;

    private ItemLevelCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("item")
                .then(Commands.literal("level")
                        .then(Commands.argument(ARG_LEVEL, IntegerArgumentType.integer(MIN_LEVEL))
                                .executes(context -> apply(context, self(context), IntegerArgumentType.getInteger(context, ARG_LEVEL)))
                                .then(Commands.argument(ARG_TARGETS, EntityArgument.players())
                                        .executes(context -> apply(context, EntityArgument.getPlayers(context, ARG_TARGETS),
                                                IntegerArgumentType.getInteger(context, ARG_LEVEL))))))
                .then(Commands.literal("clear")
                        .executes(context -> apply(context, self(context), NO_LEVEL))
                        .then(Commands.argument(ARG_TARGETS, EntityArgument.players())
                                .executes(context -> apply(context, EntityArgument.getPlayers(context, ARG_TARGETS), NO_LEVEL))))
                .then(Commands.literal("query").executes(ItemLevelCommands::query));
    }

    private static Collection<ServerPlayer> self(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return List.of(context.getSource().getPlayerOrException());
    }

    private static int apply(CommandContext<CommandSourceStack> context, Collection<ServerPlayer> players, int level) {
        CommandSourceStack source = context.getSource();
        int changed = 0;
        for (ServerPlayer player : players) {
            ItemStack stack = player.getMainHandItem();
            if (stack.isEmpty()) {
                source.sendFailure(Component.translatable(KEY + "empty", player.getDisplayName()));
                continue;
            }
            if (level == NO_LEVEL) {
                ItemScaling.removeScaling(stack);
                source.sendSuccess(() -> Component.translatable(KEY + "cleared", player.getDisplayName(), stack.getHoverName()), true);
            } else {
                ItemScaling.rescale(stack, level);
                source.sendSuccess(() -> Component.translatable(KEY + "set", player.getDisplayName(), stack.getHoverName(), level), true);
            }
            changed++;
        }
        return changed;
    }

    private static int query(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            source.sendFailure(Component.translatable(KEY + "empty", player.getDisplayName()));
            return 0;
        }
        if (!ItemScaling.isScaled(stack)) {
            source.sendSuccess(() -> Component.translatable(KEY + "none", stack.getHoverName()), false);
            return 0;
        }
        int level = ItemScaling.getScaleFactor(stack);
        source.sendSuccess(() -> Component.translatable(KEY + "query", stack.getHoverName(), level), false);
        return level;
    }
}
