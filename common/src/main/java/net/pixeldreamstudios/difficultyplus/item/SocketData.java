package net.pixeldreamstudios.difficultyplus.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.pixeldreamstudios.difficultyplus.item.bias.BiasDefinition;
import net.pixeldreamstudios.difficultyplus.item.bias.BiasTables;

public final class SocketData {
    private SocketData() {}

    private static final String BIAS_ID = "difficultyplus_bias";
    private static final String BIAS_VALUE = "difficultyplus_bias_value";
    private static final String SOCKETED_AT = "difficultyplus_socketed_at";

    public static void writeBias(ItemStack stack, BiasDefinition definition, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putString(BIAS_ID, definition.id().toString());
            tag.putInt(BIAS_VALUE, value);
        });
    }

    public static void copyBias(ItemStack from, ItemStack to) {
        CustomData source = from.get(DataComponents.CUSTOM_DATA);
        if (source == null) {
            return;
        }
        CompoundTag tag = source.copyTag();
        if (!tag.contains(BIAS_ID)) {
            return;
        }
        String id = tag.getString(BIAS_ID);
        int value = tag.getInt(BIAS_VALUE);
        CustomData.update(DataComponents.CUSTOM_DATA, to, target -> {
            target.putString(BIAS_ID, id);
            target.putInt(BIAS_VALUE, value);
        });
    }

    public static BiasDefinition definitionOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return BiasDefinition.PLAIN;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(BIAS_ID)) {
            return BiasDefinition.PLAIN;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(BIAS_ID));
        return id == null ? BiasDefinition.PLAIN : BiasTables.byId(id);
    }

    public static int biasOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0;
        }
        CompoundTag tag = data.copyTag();
        return tag.contains(BIAS_VALUE) ? tag.getInt(BIAS_VALUE) : 0;
    }

    public static boolean hasBias(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(BIAS_ID);
    }

    public static void markSocketed(ItemStack stack, long gameTime) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
                tag -> tag.putLong(SOCKETED_AT, gameTime));
    }

    public static long socketedAt(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return 0L;
        }
        CompoundTag tag = data.copyTag();
        return tag.contains(SOCKETED_AT) ? tag.getLong(SOCKETED_AT) : 0L;
    }
}
