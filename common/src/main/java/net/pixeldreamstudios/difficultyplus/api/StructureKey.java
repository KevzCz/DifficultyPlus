package net.pixeldreamstudios.difficultyplus.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public record StructureKey(ResourceLocation structureId, long center) {

    public static StructureKey of(ResourceLocation structureId, BlockPos center) {
        return new StructureKey(structureId, center.asLong());
    }

    public BlockPos centerPos() {
        return BlockPos.of(center);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", structureId.toString());
        tag.putLong("center", center);
        return tag;
    }

    public static StructureKey load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
        return id == null ? null : new StructureKey(id, tag.getLong("center"));
    }

    public String encode() {
        return structureId + "@" + center;
    }

    public static StructureKey decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        int at = encoded.lastIndexOf('@');
        if (at <= 0 || at == encoded.length() - 1) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(encoded.substring(0, at));
        if (id == null) {
            return null;
        }
        try {
            return new StructureKey(id, Long.parseLong(encoded.substring(at + 1)));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
