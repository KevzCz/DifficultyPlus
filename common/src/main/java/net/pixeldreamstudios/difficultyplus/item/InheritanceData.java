package net.pixeldreamstudios.difficultyplus.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.OptionalInt;

public final class InheritanceData {

    private static final String LEVEL = "difficultyplus_inherited_level";

    private InheritanceData() {
    }

    public static OptionalInt levelOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null || !data.contains(LEVEL)) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(data.copyTag().getInt(LEVEL));
    }

    public static void write(ItemStack stack, int level) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(LEVEL, level));
    }
}
