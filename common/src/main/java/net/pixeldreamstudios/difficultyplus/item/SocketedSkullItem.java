package net.pixeldreamstudios.difficultyplus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.internal.structure.CastRitual;
import net.pixeldreamstudios.difficultyplus.internal.structure.StructureRefresh;
import net.pixeldreamstudios.difficultyplus.internal.structure.StructureTargeting;
import net.pixeldreamstudios.difficultyplus.item.bias.BiasDefinition;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class SocketedSkullItem extends Item {

    private final GemKind kind;

    public SocketedSkullItem(Properties properties, GemKind kind) {
        super(properties);
        this.kind = kind;
    }

    public GemKind kind() {
        return kind;
    }

    public int castTicks() {
        return kind.rerolls() ? 60 : 40;
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack skull, Slot slot, ClickAction action,
                                          Player player) {
        if (action != ClickAction.SECONDARY || !slot.getItem().isEmpty()
                || !slot.allowModification(player)) {
            return false;
        }
        if (isSpent(skull)) {
            refuse(player);
            return true;
        }
        ItemStack gem = detach(skull, player);
        slot.set(gem);
        skull.shrink(1);
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack skull, ItemStack other, Slot slot,
                                            ClickAction action, Player player,
                                            SlotAccess cursor) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }
        if (other.isEmpty()) {
            if (isSpent(skull)) {
                refuse(player);
                return true;
            }
            ItemStack gem = detach(skull, player);
            cursor.set(gem);
            slot.set(new ItemStack(DPItems.HOLLOW_SKULL.get()));
            return true;
        }
        if (other.getItem() instanceof GemItem) {
            refuse(player);
            return true;
        }
        return false;
    }

    private static boolean isSpent(ItemStack skull) {
        return skull.getDamageValue() > 0;
    }

    private static void refuse(Player player) {
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.VAULT_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 0.7f);
    }

    private ItemStack detach(ItemStack skull, Player player) {
        ItemStack gem = new ItemStack(kind == GemKind.SAPPHIRE
                ? DPItems.SAPPHIRE_OF_RENEWAL.get()
                : DPItems.GARNET_OF_FATE.get());
        SocketData.copyBias(skull, gem);

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.7f, 1.1f);
        return gem;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer server)) {
            return InteractionResultHolder.consume(stack);
        }

        Optional<StructureStart> target = StructureTargeting.resolve(serverLevel, player);
        if (target.isEmpty()) {
            server.displayClientMessage(
                    Component.translatable("message.difficultyplus.no_structure")
                            .withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }

        StructureRefresh.Result check = StructureRefresh.inspect(serverLevel, target.get());
        if (!check.ready()) {
            CastRitual.reportRefusal(server, serverLevel, target.get(), check);
            return InteractionResultHolder.fail(stack);
        }

        if (kind.rerolls() && !StructureRefresh.canReroll(serverLevel, target.get())) {
            server.displayClientMessage(
                    Component.translatable("message.difficultyplus.no_range")
                            .withStyle(ChatFormatting.RED), true);
            serverLevel.playSound(null, player.blockPosition(),
                    SoundEvents.VAULT_DEACTIVATE, SoundSource.PLAYERS, 0.7f, 0.6f);
            return InteractionResultHolder.fail(stack);
        }

        CastRitual.begin(server, serverLevel, target.get(), this, hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        if (kind == GemKind.GARNET && SocketData.hasBias(stack)) {
            BiasDefinition definition = SocketData.definitionOf(stack);
            int bias = SocketData.biasOf(stack);
            tooltip.add(Component.translatable(definition.name())
                    .withStyle(style -> style.withColor(definition.colour())));
            tooltip.add(Component.translatable("tooltip.difficultyplus.bias_shift",
                            bias >= 0 ? "+" + bias : String.valueOf(bias))
                    .withStyle(bias >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
            if (definition.pierceFloor()) {
                tooltip.add(Component.translatable("tooltip.difficultyplus.pierce_floor")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
            if (definition.pierceCeiling()) {
                tooltip.add(Component.translatable("tooltip.difficultyplus.pierce_ceiling")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
        }
        if (isSpent(stack)) {
            tooltip.add(Component.translatable("tooltip.difficultyplus.gem_fused")
                    .withStyle(ChatFormatting.DARK_RED));
        }
        tooltip.add(Component.translatable(kind.rerolls()
                        ? "tooltip.difficultyplus.skull_of_fate"
                        : "tooltip.difficultyplus.skull_of_renewal")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
