package net.pixeldreamstudios.difficultyplus.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.pixeldreamstudios.difficultyplus.api.DifficultyPlusAPI;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.item.bias.BiasDefinition;
import net.pixeldreamstudios.difficultyplus.item.bias.BiasTables;

import java.util.ArrayList;
import java.util.List;

public final class ReliquaryLoot {
    private ReliquaryLoot() {}

    public static void injectInto(Container container, LootParams params, long seed) {
        Vec3 origin = params.getParamOrNull(LootContextParams.ORIGIN);
        ServerLevel level = params.getLevel();
        if (level == null || origin == null) {
            return;
        }

        BlockPos pos = BlockPos.containing(origin);
        int zoneLevel = DifficultyPlusAPI.levelAt(level, pos).orElse(0);
        if (zoneLevel <= 0) {
            return;
        }

        RandomSource random = RandomSource.create(seed ^ pos.asLong());
        List<ItemStack> drops = roll(random, zoneLevel);
        for (ItemStack drop : drops) {
            place(container, drop, random);
        }
    }

    private static List<ItemStack> roll(RandomSource random, int zoneLevel) {
        GlobalData.StructureRefresh config = ConfigBridge.global().structure_refresh;
        List<ItemStack> drops = new ArrayList<>();

        if (zoneLevel >= config.hollow_skull_min_level
                && random.nextFloat() < config.hollow_skull_chance) {
            drops.add(new ItemStack(DPItems.HOLLOW_SKULL.get()));
        }
        if (zoneLevel >= config.sapphire_min_level
                && random.nextFloat() < config.sapphire_chance) {
            drops.add(new ItemStack(DPItems.SAPPHIRE_OF_RENEWAL.get()));
        }
        if (zoneLevel >= config.garnet_min_level
                && random.nextFloat() < config.garnet_chance) {
            drops.add(garnet(random, zoneLevel));
        }
        return drops;
    }

    public static ItemStack garnet(RandomSource random, int zoneLevel) {
        ItemStack stack = new ItemStack(DPItems.GARNET_OF_FATE.get());
        BiasDefinition definition = BiasTables.pick(random, zoneLevel);
        SocketData.writeBias(stack, definition, definition.roll(random));
        return stack;
    }

    private static void place(Container container, ItemStack stack, RandomSource random) {
        List<Integer> empty = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).isEmpty()) {
                empty.add(slot);
            }
        }
        if (empty.isEmpty()) {
            return;
        }
        container.setItem(empty.get(random.nextInt(empty.size())), stack);
    }
}
