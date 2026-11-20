package net.pixeldreamstudios.difficultyplus.item;

import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.OptionalInt;

public class InheritanceGemItem extends GemItem {

    private static final String KEY = "difficultyplus.amethyst_of_inheritance.";
    private static final float REFUSE_VOLUME = 0.6f;
    private static final float REFUSE_PITCH = 0.7f;
    private static final float DRAW_VOLUME = 0.8f;
    private static final float DRAW_PITCH = 0.6f;
    private static final float GIVE_VOLUME = 0.8f;
    private static final float GIVE_PITCH = 1.4f;

    public InheritanceGemItem(Properties properties) {
        super(properties, GemKind.AMETHYST);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack gem, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }
        ItemStack target = slot.getItem();
        if (target.isEmpty() || target.getItem() instanceof GemItem) {
            return false;
        }
        if (!(player instanceof ServerPlayer server)) {
            return true;
        }
        if (gem.getCount() > 1) {
            refuse(server, "one_at_a_time");
            return true;
        }
        OptionalInt stored = InheritanceData.levelOf(gem);
        if (stored.isEmpty()) {
            draw(server, gem, target);
        } else {
            give(server, gem, target, stored.getAsInt());
        }
        return true;
    }

    private static void draw(ServerPlayer player, ItemStack gem, ItemStack target) {
        int level = ItemScaling.isScaled(target) ? ItemScaling.getScaleFactor(target) : 0;
        if (level <= 0) {
            refuse(player, "no_level");
            return;
        }
        ItemScaling.removeScaling(target);
        InheritanceData.write(gem, level);
        play(player, SoundEvents.AMETHYST_BLOCK_RESONATE, DRAW_VOLUME, DRAW_PITCH);
        player.displayClientMessage(Component.translatable(KEY + "drawn", level, target.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static void give(ServerPlayer player, ItemStack gem, ItemStack target, int level) {
        if (!target.isDamageableItem() && !ItemScaling.isScaled(target)) {
            refuse(player, "not_gear");
            return;
        }
        int existing = ItemScaling.isScaled(target) ? ItemScaling.getScaleFactor(target) : 0;
        if (existing >= level) {
            refuse(player, "already_higher");
            return;
        }
        ItemScaling.rescale(target, level);
        gem.shrink(1);
        play(player, SoundEvents.AMETHYST_BLOCK_CHIME, GIVE_VOLUME, GIVE_PITCH);
        play(player, SoundEvents.END_PORTAL_FRAME_FILL, GIVE_VOLUME, GIVE_PITCH);
        player.displayClientMessage(Component.translatable(KEY + "given", target.getHoverName(), level).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    private static void refuse(ServerPlayer player, String reason) {
        play(player, SoundEvents.VAULT_DEACTIVATE, REFUSE_VOLUME, REFUSE_PITCH);
        player.displayClientMessage(Component.translatable(KEY + reason).withStyle(ChatFormatting.RED), true);
    }

    private static void play(ServerPlayer player, SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, volume, pitch);
    }

    @Override
    public Component getName(ItemStack stack) {
        OptionalInt level = InheritanceData.levelOf(stack);
        return level.isPresent() ? Component.translatable(KEY + "charged_name", super.getName(stack), level.getAsInt()) : super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        OptionalInt level = InheritanceData.levelOf(stack);
        if (level.isPresent()) {
            tooltip.add(Component.translatable(KEY + "holds", level.getAsInt()).withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.add(Component.translatable(KEY + "give_hint", level.getAsInt()).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.add(Component.translatable(KEY + "draw_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
