package net.pixeldreamstudios.difficultyplus.internal.level;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.pixeldreamstudios.difficultyplus.api.StructureKey;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public final class LevelStore extends SavedData {

    private static final String ID = "difficultyplus_levels";

    private final Map<StructureKey, Integer> levels = new HashMap<>();

    public static LevelStore get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(LevelStore::new, LevelStore::load, null), ID);
    }

    public OptionalInt indexed(StructureKey key) {
        Integer value = levels.get(key);
        return value == null ? OptionalInt.empty() : OptionalInt.of(value);
    }

    public void index(StructureKey key, int level) {
        Integer previous = levels.put(key, level);
        if (previous == null || previous != level) {
            setDirty();
        }
    }

    public void forget(StructureKey key) {
        if (levels.remove(key) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        levels.forEach((key, level) -> {
            CompoundTag entry = key.save();
            entry.putInt("level", level);
            list.add(entry);
        });
        tag.put("entries", list);
        return tag;
    }

    private static LevelStore load(CompoundTag tag, HolderLookup.Provider registries) {
        LevelStore store = new LevelStore();
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            StructureKey key = StructureKey.load(entry);
            if (key != null) {
                store.levels.put(key, entry.getInt("level"));
            }
        }
        return store;
    }
}
