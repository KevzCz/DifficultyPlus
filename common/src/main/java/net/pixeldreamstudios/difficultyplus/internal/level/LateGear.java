package net.pixeldreamstudios.difficultyplus.internal.level;

import net.dungeon_difficulty.logic.EntityDifficultyScalable;
import net.dungeon_difficulty.logic.ItemScaling;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;

public final class LateGear {
    private LateGear() {}

    public static void tick(LivingEntity entity) {
        if (entity instanceof Player || entity.level().isClientSide()) {
            return;
        }
        GlobalData.LateEquipment config = ConfigBridge.global().late_equipment;
        if (config == null || !config.enabled
                || !(entity instanceof LateGearAccess tracked)
                || tracked.difficultyplus$gearScaled()) {
            return;
        }
        if (entity.tickCount > config.scale_within_ticks) {
            tracked.difficultyplus$setGearScaled(true);
            DifficultyPlus.debug(() -> {
                StringBuilder census = new StringBuilder();
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack held = entity.getItemBySlot(slot);
                    census.append(' ').append(slot.getName()).append('=')
                            .append(held.isEmpty() ? "-" : held.getItem() + ":"
                                    + ItemScaling.getScaleFactor(held));
                }
                return String.format("late gear window closed %s tick=%d%s",
                        entity.getType().getDescriptionId(), entity.tickCount, census);
            });
            return;
        }
        if (!(entity instanceof EntityDifficultyScalable scalable)) {
            return;
        }
        int level = scalable.getScalingLevel();
        if (level <= 0) {
            DifficultyPlus.debugOnce("lategear-nolevel-" + entity.getUUID(), () -> String.format(
                    "late gear no level %s tick=%d mainhand=%s",
                    entity.getType().getDescriptionId(), entity.tickCount,
                    entity.getItemBySlot(EquipmentSlot.MAINHAND).getItem()));
            return;
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            scaleStack(entity, entity.getItemBySlot(slot), level, slot.getName());
        }
        for (ItemStack stack : entity.getHandSlots()) {
            scaleStack(entity, stack, level, "hand");
        }
        for (ItemStack stack : entity.getArmorSlots()) {
            scaleStack(entity, stack, level, "armor");
        }
    }

    private static boolean scaleStack(LivingEntity entity, ItemStack stack, int level, String where) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        int existing = ItemScaling.isScaled(stack) ? ItemScaling.getScaleFactor(stack) : 0;
        if (existing >= level) {
            return false;
        }
        ItemScaling.rescale(stack, level);
        DifficultyPlus.debug(() -> String.format(
                "late gear %s %s item=%s tick=%d was=%d now=%d",
                entity.getType().getDescriptionId(), where, stack.getItem(),
                entity.tickCount, existing, ItemScaling.getScaleFactor(stack)));
        return true;
    }
}
