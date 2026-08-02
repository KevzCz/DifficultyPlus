package net.pixeldreamstudios.difficultyplus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class HollowSkullItem extends Item {

    public HollowSkullItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack skull, ItemStack other, Slot slot,
                                            ClickAction action, Player player,
                                            SlotAccess cursor) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }
        if (!(other.getItem() instanceof GemItem gem)) {
            return false;
        }

        if (skull.getCount() > 1) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.VAULT_DEACTIVATE, SoundSource.PLAYERS, 0.6f, 0.7f);
            return true;
        }

        ItemStack socketed = new ItemStack(gem.kind() == GemKind.SAPPHIRE
                ? DPItems.SKULL_OF_RENEWAL.get()
                : DPItems.SKULL_OF_FATE.get());
        SocketData.copyBias(other, socketed);
        SocketData.markSocketed(socketed, player.level().getGameTime());

        other.shrink(1);
        slot.set(socketed);

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8f, 1.4f);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.PLAYERS, 0.7f, 1.5f);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.difficultyplus.hollow_skull")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
