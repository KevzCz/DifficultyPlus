package net.pixeldreamstudios.difficultyplus.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.pixeldreamstudios.difficultyplus.internal.level.StructureLevelAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureStart.class)
public class StructureStartMixin implements StructureLevelAccess {

    @Unique
    private int difficultyplus$level = StructureLevelAccess.NONE;
    @Unique
    private int difficultyplus$salt = 0;
    @Unique
    private String difficultyplus$source = null;
    @Unique
    private long difficultyplus$refreshedAt = 0L;
    @Unique
    private long[] difficultyplus$placements = null;
    @Unique
    private int difficultyplus$bias = 0;
    @Unique
    private int difficultyplus$pierce = 0;

    @Override
    public int difficultyplus$bias() {
        return difficultyplus$bias;
    }

    @Override
    public void difficultyplus$setBias(int bias) {
        this.difficultyplus$bias = bias;
    }

    @Override
    public int difficultyplus$pierce() {
        return difficultyplus$pierce;
    }

    @Override
    public void difficultyplus$setPierce(int flags) {
        this.difficultyplus$pierce = flags;
    }

    @Override
    public long difficultyplus$refreshedAt() {
        return difficultyplus$refreshedAt;
    }

    @Override
    public void difficultyplus$setRefreshedAt(long gameTime) {
        this.difficultyplus$refreshedAt = gameTime;
    }

    @Override
    public long[] difficultyplus$placements() {
        return difficultyplus$placements;
    }

    @Override
    public void difficultyplus$setPlacements(long[] positions) {
        this.difficultyplus$placements = positions == null || positions.length == 0 ? null : positions;
    }

    @Override
    public int difficultyplus$level() {
        return difficultyplus$level;
    }

    @Override
    public void difficultyplus$setLevel(int level) {
        this.difficultyplus$level = level;
    }

    @Override
    public int difficultyplus$salt() {
        return difficultyplus$salt;
    }

    @Override
    public void difficultyplus$setSalt(int salt) {
        this.difficultyplus$salt = salt;
    }

    @Override
    public String difficultyplus$source() {
        return difficultyplus$source;
    }

    @Override
    public void difficultyplus$setSource(String source) {
        this.difficultyplus$source = source;
    }

    @Inject(method = "createTag", at = @At("RETURN"))
    private void difficultyplus$write(StructurePieceSerializationContext context,
                                      ChunkPos chunkPos,
                                      CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();

        if (tag == null || !tag.contains("Children")) {
            return;
        }

        boolean hasLevel = difficultyplus$level != StructureLevelAccess.NONE;
        if (!hasLevel && difficultyplus$salt == 0 && difficultyplus$source == null
                && difficultyplus$refreshedAt == 0L && difficultyplus$placements == null) {
            return;
        }
        CompoundTag ours = new CompoundTag();
        if (hasLevel) {
            ours.putInt("level", difficultyplus$level);
        }
        ours.putInt("salt", difficultyplus$salt);
        if (difficultyplus$source != null) {
            ours.putString("source", difficultyplus$source);
        }
        if (difficultyplus$refreshedAt != 0L) {
            ours.putLong("refreshed_at", difficultyplus$refreshedAt);
        }
        if (difficultyplus$placements != null) {
            ours.putLongArray("placements", difficultyplus$placements);
        }
        if (difficultyplus$bias != 0) {
            ours.putInt("bias", difficultyplus$bias);
        }
        if (difficultyplus$pierce != 0) {
            ours.putInt("pierce", difficultyplus$pierce);
        }
        tag.put("difficultyplus", ours);
    }

    @Inject(method = "loadStaticStart", at = @At("RETURN"))
    private static void difficultyplus$read(StructurePieceSerializationContext context,
                                            CompoundTag tag,
                                            long seed,
                                            CallbackInfoReturnable<StructureStart> cir) {
        StructureStart start = cir.getReturnValue();
        if (start == null || tag == null || !tag.contains("difficultyplus")) {
            return;
        }
        CompoundTag ours = tag.getCompound("difficultyplus");
        StructureLevelAccess access = (StructureLevelAccess) (Object) start;
        access.difficultyplus$setLevel(ours.contains("level") ? ours.getInt("level") : StructureLevelAccess.NONE);
        access.difficultyplus$setSalt(ours.getInt("salt"));
        if (ours.contains("source")) {
            access.difficultyplus$setSource(ours.getString("source"));
        }
        if (ours.contains("refreshed_at")) {
            access.difficultyplus$setRefreshedAt(ours.getLong("refreshed_at"));
        }
        if (ours.contains("placements")) {
            access.difficultyplus$setPlacements(ours.getLongArray("placements"));
        }
        if (ours.contains("bias")) {
            access.difficultyplus$setBias(ours.getInt("bias"));
        }
        if (ours.contains("pierce")) {
            access.difficultyplus$setPierce(ours.getInt("pierce"));
        }
    }
}
