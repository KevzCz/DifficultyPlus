package net.pixeldreamstudios.difficultyplus.config;

import net.minecraft.core.BlockPos;

public class DistanceScaling {

    public boolean enabled = false;

    public boolean override = false;

    public boolean scale_fallback = true;

    public int grace_radius = 0;

    public int per_blocks = 1000;

    public int add_level = 1;

    public int max_add = 0;

    public boolean isActive() {
        return enabled && per_blocks > 0 && add_level != 0;
    }

    public int addedFor(BlockPos origin, BlockPos pos) {
        if (!isActive() || origin == null || pos == null) {
            return 0;
        }
        double dx = pos.getX() - origin.getX();
        double dz = pos.getZ() - origin.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz) - Math.max(0, grace_radius);
        if (distance <= 0) {
            return 0;
        }
        long steps = (long) (distance / per_blocks);
        long added = steps * add_level;
        if (max_add > 0) {
            added = Math.min(added, max_add);
        }
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, added));
    }
}
