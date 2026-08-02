package net.pixeldreamstudios.difficultyplus.item;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

import java.util.ArrayList;
import java.util.List;

public final class DPItems {
    private DPItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(DifficultyPlus.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> HOLLOW_SKULL = ITEMS.register("hollow_skull",
            () -> new HollowSkullItem(new Item.Properties()
                    .stacksTo(16)
                    .rarity(Rarity.UNCOMMON)));

    public static final RegistrySupplier<Item> SAPPHIRE_OF_RENEWAL =
            ITEMS.register("sapphire_of_renewal",
                    () -> new GemItem(new Item.Properties()
                            .stacksTo(16)
                            .rarity(Rarity.RARE), GemKind.SAPPHIRE));

    public static final RegistrySupplier<Item> GARNET_OF_FATE =
            ITEMS.register("garnet_of_fate",
                    () -> new GemItem(new Item.Properties()
                            .stacksTo(16)
                            .rarity(Rarity.RARE), GemKind.GARNET));

    public static final RegistrySupplier<Item> SKULL_OF_RENEWAL =
            ITEMS.register("skull_of_renewal",
                    () -> new SocketedSkullItem(new Item.Properties()
                            .stacksTo(1)
                            .durability(3)
                            .rarity(Rarity.EPIC), GemKind.SAPPHIRE));

    public static final RegistrySupplier<Item> SKULL_OF_FATE =
            ITEMS.register("skull_of_fate",
                    () -> new SocketedSkullItem(new Item.Properties()
                            .stacksTo(1)
                            .durability(1)
                            .rarity(Rarity.EPIC), GemKind.GARNET));

    public static final List<RegistrySupplier<Item>> BLUE_SHARDS = shards("blue");

    public static final List<RegistrySupplier<Item>> RED_SHARDS = shards("red");

    private static List<RegistrySupplier<Item>> shards(String colour) {
        List<RegistrySupplier<Item>> out = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            out.add(ITEMS.register("shard_" + colour + "_" + i,
                    () -> new Item(new Item.Properties().stacksTo(64))));
        }
        return List.copyOf(out);
    }

    public static List<RegistrySupplier<Item>> shardsFor(GemKind kind) {
        return kind == GemKind.SAPPHIRE ? BLUE_SHARDS : RED_SHARDS;
    }

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(DifficultyPlus.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> TAB = TABS.register("reliquary",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup.difficultyplus.reliquary"),
                    () -> SKULL_OF_FATE.isPresent()
                            ? new ItemStack(SKULL_OF_FATE.get())
                            : new ItemStack(Items.WITHER_SKELETON_SKULL)));

    public static void register() {
        ITEMS.register();
        TABS.register();

        CreativeTabRegistry.append(TAB,
                HOLLOW_SKULL, SAPPHIRE_OF_RENEWAL, GARNET_OF_FATE,
                SKULL_OF_RENEWAL, SKULL_OF_FATE);
    }
}
