package net.pixeldreamstudios.difficultyplus.internal.structure;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.internal.level.StructureLevelAccess;
import net.pixeldreamstudios.difficultyplus.item.GemKind;
import net.pixeldreamstudios.difficultyplus.item.SocketData;
import net.pixeldreamstudios.difficultyplus.item.SocketedSkullItem;
import net.pixeldreamstudios.difficultyplus.item.bias.BiasDefinition;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SurveyTracker {
    private SurveyTracker() {}

    private static final int INTERVAL = 10;

    private static final Map<UUID, List<BlockPos>> LAST_BLOCKED = new ConcurrentHashMap<>();

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % INTERVAL != 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            ItemStack held = heldSkull(player);
            if (held.isEmpty()) {
                StructureOutline.clear(player);
                BlockHighlight.clear(player);
                continue;
            }
            if (StructureOutline.isLocked(player)) {
                continue;
            }
            survey(level, player, held);
        }
    }

    public static void onLeave(ServerPlayer player) {
        StructureOutline.clear(player);
        BlockHighlight.clear(player);
        LAST_BLOCKED.remove(player.getUUID());
    }

    public static void clearAll() {
        StructureOutline.clearAll();
        BlockHighlight.clearAll();
        LAST_BLOCKED.clear();
    }

    public static int sweepStrays(ServerLevel level) {
        clearAll();
        int removed = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity.getTags().contains(StructureOutline.TRANSIENT_TAG)) {
                entity.discard();
                removed++;
            }
        }
        return removed;
    }

    private static ItemStack heldSkull(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof SocketedSkullItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void survey(ServerLevel level, ServerPlayer player, ItemStack held) {
        Optional<StructureStart> target = StructureTargeting.resolve(level, player);
        if (target.isEmpty()) {
            StructureOutline.clear(player);
            BlockHighlight.clear(player);
            LAST_BLOCKED.remove(player.getUUID());
            player.displayClientMessage(
                    Component.translatable("message.difficultyplus.no_structure")
                            .withStyle(ChatFormatting.DARK_GRAY), true);
            return;
        }

        StructureStart start = target.get();
        StructureRefresh.Result result = StructureRefresh.inspect(level, start);
        int colour = result.ready() ? GemKind.SURVEY_COLOUR : GemKind.REFUSED_COLOUR;

        StructureOutline.show(player, level, start, colour);

        if (result.state() == StructureRefresh.Result.State.BLOCKED) {
            List<BlockPos> blocked = result.keyBlocks();
            if (!blocked.equals(LAST_BLOCKED.get(player.getUUID()))) {
                LAST_BLOCKED.put(player.getUUID(), List.copyOf(blocked));
                BlockHighlight.outline(level, player, blocked);
            }
        } else if (LAST_BLOCKED.remove(player.getUUID()) != null) {
            BlockHighlight.clear(player);
        }

        player.displayClientMessage(describe(level, start, result, held), true);
    }

    private static Component describe(ServerLevel level, StructureStart start,
                                      StructureRefresh.Result result, ItemStack held) {
        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceLocation id = registry.getKey(start.getStructure());
        BoundingBox box = start.getBoundingBox();

        String name = id == null ? "structure" : id.getPath().replace('_', ' ');
        StructureLevelAccess access = (StructureLevelAccess) (Object) start;
        int stored = access.difficultyplus$level();

        Component base = Component.literal(capitalise(name))
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" · " + box.getXSpan() + "×" + box.getYSpan()
                        + "×" + box.getZSpan()).withStyle(ChatFormatting.DARK_GRAY));

        if (stored != StructureLevelAccess.NONE) {
            base = base.copy().append(Component.literal(" · lv " + stored)
                    .withStyle(ChatFormatting.AQUA));
        }

        Component state = switch (result.state()) {
            case READY -> preview(held);
            case DISABLED -> Component.literal(" · disabled").withStyle(ChatFormatting.RED);
            case EXCLUDED -> Component.literal(" · blacklisted").withStyle(ChatFormatting.RED);
            case COOLING -> Component.literal(" · ready in " + format(result.secondsLeft()))
                    .withStyle(ChatFormatting.GOLD);
            case BLOCKED -> Component.literal(" · " + result.keyBlocks().size() + " key block(s)")
                    .withStyle(ChatFormatting.RED);
            default -> Component.empty();
        };
        return base.copy().append(state);
    }

    private static Component preview(ItemStack held) {
        if (!(held.getItem() instanceof SocketedSkullItem skull) || !skull.kind().rerolls()) {
            return Component.literal(" · ready").withStyle(ChatFormatting.GREEN);
        }
        BiasDefinition definition = SocketData.definitionOf(held);
        int bias = SocketData.biasOf(held);
        Component text = Component.literal(" · reroll")
                .withStyle(ChatFormatting.GREEN);
        if (bias != 0) {
            text = text.copy().append(Component.literal(bias > 0 ? " +" + bias : " " + bias)
                    .withStyle(bias > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        }
        if (definition.pierceFloor() || definition.pierceCeiling()) {
            text = text.copy().append(Component.literal(" ⚠")
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
        return text;
    }

    private static String format(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        }
        return (seconds / 60) + "m";
    }

    private static String capitalise(String text) {
        if (text.isEmpty()) {
            return text;
        }
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}
