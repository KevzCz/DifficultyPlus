package net.pixeldreamstudios.difficultyplus.internal.zone;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.config.GlobalData;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;
import net.pixeldreamstudios.difficultyplus.internal.structure.StructureTargeting;

public final class StructureContainment {
    private StructureContainment() {}

    public static boolean contains(StructureStart start, BlockPos pos) {
        if (start == null || !start.isValid()) {
            return false;
        }
        GlobalData.StructureZones config = ConfigBridge.global().structure_zones;
        if (config == null || !config.pieces_only) {
            return start.getBoundingBox().isInside(pos);
        }
        int margin = Math.max(0, config.piece_margin);
        return start.getBoundingBox().inflatedBy(margin).isInside(pos)
                && StructureTargeting.nearPiece(start, pos, margin);
    }
}
