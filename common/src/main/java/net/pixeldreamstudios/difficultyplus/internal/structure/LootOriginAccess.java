package net.pixeldreamstudios.difficultyplus.internal.structure;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;

public interface LootOriginAccess {

    ResourceKey<LootTable> difficultyplus$originalLootTable();

    void difficultyplus$setOriginalLootTable(ResourceKey<LootTable> table);
}
