package net.pixeldreamstudios.difficultyplus.internal.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StructureTargeting {
    private StructureTargeting() {}

    public static final double RAY_REACH = 96.0;

    private static final int SWEEP_CHUNKS = 6;

    private static final int PIECE_SLACK = 3;

    private static final double SIGHT_EPSILON = 4.0;

    public static Optional<StructureStart> resolve(ServerLevel level, Player player) {
        Optional<StructureStart> enclosing = enclosing(level, player.blockPosition());
        if (enclosing.isPresent()) {
            return enclosing;
        }
        return rayPick(level, player);
    }

    public static Optional<StructureStart> enclosing(ServerLevel level, BlockPos pos) {
        StructureStart smallest = null;
        long smallestVolume = Long.MAX_VALUE;

        for (StructureStart start : level.structureManager()
                .startsForStructure(new ChunkPos(pos), s -> true)) {
            if (!start.isValid() || !start.getBoundingBox().isInside(pos)) {
                continue;
            }
            if (!nearPiece(start, pos)) {
                continue;
            }
            long volume = volumeOf(start.getBoundingBox());
            if (volume < smallestVolume) {
                smallestVolume = volume;
                smallest = start;
            }
        }
        return Optional.ofNullable(smallest);
    }

    public static BoundingBox pieceBox(StructurePiece piece) {
        BoundingBox box = piece.getBoundingBox();
        if (!(piece instanceof TemplateStructurePiece template)) {
            return box;
        }
        int drift = template.templatePosition().getY() - box.minY();
        return drift == 0 ? box : box.moved(0, drift, 0);
    }

    private static boolean nearPiece(StructureStart start, BlockPos pos) {
        for (StructurePiece piece : start.getPieces()) {
            if (pieceBox(piece).inflatedBy(PIECE_SLACK).isInside(pos)) {
                return true;
            }
        }
        return false;
    }

    private static Optional<StructureStart> rayPick(ServerLevel level, Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getViewVector(1.0f).scale(RAY_REACH));

        StructureStart nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (StructureStart start : candidates(level, player.blockPosition())) {
            Optional<Vec3> hit = clipPieces(start, from, to);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = from.distanceToSqr(hit.get());
            if (distance >= nearestDistance) {
                continue;
            }
            if (!hasLineOfSight(level, player, from, hit.get(), distance)) {
                continue;
            }
            nearestDistance = distance;
            nearest = start;
        }
        return Optional.ofNullable(nearest);
    }

    private static boolean hasLineOfSight(ServerLevel level, Player player,
                                          Vec3 from, Vec3 hit, double hitDistance) {
        BlockHitResult blocked = level.clip(new ClipContext(
                from, hit, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blocked.getType() == HitResult.Type.MISS) {
            return true;
        }
        return from.distanceToSqr(blocked.getLocation()) + SIGHT_EPSILON >= hitDistance;
    }

    private static Optional<Vec3> clipPieces(StructureStart start, Vec3 from, Vec3 to) {
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;

        for (StructurePiece piece : start.getPieces()) {
            Optional<Vec3> hit = aabbOf(pieceBox(piece)).clip(from, to);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = from.distanceToSqr(hit.get());
            if (distance < bestDistance) {
                bestDistance = distance;
                best = hit.get();
            }
        }
        return Optional.ofNullable(best);
    }

    private static List<StructureStart> candidates(ServerLevel level, BlockPos origin) {
        List<StructureStart> found = new ArrayList<>();
        int centreX = origin.getX() >> 4;
        int centreZ = origin.getZ() >> 4;

        for (int x = centreX - SWEEP_CHUNKS; x <= centreX + SWEEP_CHUNKS; x++) {
            for (int z = centreZ - SWEEP_CHUNKS; z <= centreZ + SWEEP_CHUNKS; z++) {
                if (level.getChunkSource().getChunkNow(x, z) == null) {
                    continue;
                }
                for (StructureStart start : level.structureManager()
                        .startsForStructure(new ChunkPos(x, z), s -> true)) {
                    if (start.isValid() && !containsIdentity(found, start)) {
                        found.add(start);
                    }
                }
            }
        }
        return found;
    }

    private static boolean containsIdentity(List<StructureStart> list, StructureStart target) {
        for (StructureStart start : list) {
            if (start == target) {
                return true;
            }
        }
        return false;
    }

    private static long volumeOf(BoundingBox box) {
        return (long) box.getXSpan() * box.getYSpan() * box.getZSpan();
    }

    public static AABB aabbOf(BoundingBox box) {
        return new AABB(box.minX(), box.minY(), box.minZ(),
                box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
    }
}
