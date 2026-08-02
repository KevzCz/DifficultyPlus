package net.pixeldreamstudios.difficultyplus.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.pixeldreamstudios.difficultyplus.item.bias.BiasDefinition;

import java.util.List;

public class GemItem extends Item {

    private final GemKind kind;

    public GemItem(Properties properties, GemKind kind) {
        super(properties);
        this.kind = kind;
    }

    public GemKind kind() {
        return kind;
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
        tooltip.add(Component.translatable(kind == GemKind.SAPPHIRE
                        ? "tooltip.difficultyplus.sapphire_hint"
                        : "tooltip.difficultyplus.garnet_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
