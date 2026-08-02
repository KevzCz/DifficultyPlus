package net.pixeldreamstudios.difficultyplus.internal.structure;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.internal.ConfigBridge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RefreshQueue {
    private RefreshQueue() {}

    private static final List<Job> JOBS = new ArrayList<>();

    private static final int TICKET_RADIUS = 1;

    private static final int WINDOW = 8;

    private static final int MAX_JOB_TICKS = 20 * 60;

    public interface Completion {
        void finished(int chunks, int blocksRemoved, int entitiesCleared);
    }

    private static final class Job {
        ServerLevel level;
        StructureStart start;
        BoundingBox box;
        RandomSource random;
        LongList chunks;
        LongSet ticketed = new LongOpenHashSet();
        int cursor;
        int placed;
        int removed;
        int cleared;
        int age;
        long[] placements;
        String ownerKey;
        final Set<UUID> doomed = new HashSet<>();
        final Set<UUID> spawned = new HashSet<>();
        Completion onDone;
    }

    public static void submit(ServerLevel level, StructureStart start, LongList chunks,
                              RandomSource random, long[] placements, String ownerKey,
                              Vec3 origin, Completion onDone) {
        Job job = new Job();
        job.level = level;
        job.start = start;
        job.box = start.getBoundingBox();
        job.random = random;
        job.chunks = sortedFrom(chunks, origin);
        job.placements = placements;
        job.ownerKey = ownerKey;
        job.onDone = onDone;

        if (ownerKey != null) {
            AABB area = new AABB(
                    job.box.minX(), job.box.minY(), job.box.minZ(),
                    job.box.maxX() + 1, job.box.maxY() + 1, job.box.maxZ() + 1);
            for (Entity entity : level.getEntities((Entity) null, area,
                    e -> !(e instanceof Player))) {
                if (OwnershipStamp.ownedBy(entity, ownerKey)
                        && job.box.isInside(entity.blockPosition())) {
                    job.doomed.add(entity.getUUID());
                }
            }
        }
        JOBS.add(job);
    }

    private static LongList sortedFrom(LongList chunks, Vec3 origin) {
        int originX = ((int) Math.floor(origin.x)) >> 4;
        int originZ = ((int) Math.floor(origin.z)) >> 4;

        List<Long> boxed = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            boxed.add(chunks.getLong(i));
        }
        boxed.sort((a, b) -> Long.compare(distanceSqr(a, originX, originZ),
                distanceSqr(b, originX, originZ)));

        LongList sorted = new LongArrayList(boxed.size());
        for (long value : boxed) {
            sorted.add(value);
        }
        return sorted;
    }

    private static long distanceSqr(long packed, int originX, int originZ) {
        ChunkPos pos = new ChunkPos(packed);
        long dx = pos.x - originX;
        long dz = pos.z - originZ;
        return dx * dx + dz * dz;
    }

    public static void exempt(Entity entity) {
        if (JOBS.isEmpty()) {
            return;
        }
        for (Job job : JOBS) {
            job.spawned.add(entity.getUUID());
        }
    }

    public static boolean isBusy(StructureStart start) {
        for (Job job : JOBS) {
            if (job.start == start) {
                return true;
            }
        }
        return false;
    }

    public static void tick(ServerLevel level) {
        if (JOBS.isEmpty()) {
            return;
        }
        int budget = Math.max(1, ConfigBridge.global().structure_refresh.chunks_per_tick);

        JOBS.removeIf(job -> {
            if (job.level != level) {
                return false;
            }
            job.age++;
            if (job.age > MAX_JOB_TICKS) {
                DifficultyPlus.LOGGER.warn(
                        "Difficulty+ refresh exceeded {} ticks with {} of {} chunk(s) done — releasing",
                        MAX_JOB_TICKS, job.cursor, job.chunks.size());
                finish(job);
                return true;
            }

            requestWindow(level, job);

            int done = 0;
            while (done < budget && job.cursor < job.chunks.size()) {
                ChunkPos chunk = new ChunkPos(job.chunks.getLong(job.cursor));
                if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null) {
                    break;
                }

                job.cursor++;
                job.removed += clearBlocksIn(level, job, chunk);
                job.cleared += clearEntitiesIn(level, job, chunk);
                job.start.placeInChunk(level, level.structureManager(),
                        level.getChunkSource().getGenerator(), job.random,
                        clip(chunk, job.box), chunk);
                job.placed++;
                release(level, job, chunk);
                done++;
            }

            if (job.cursor < job.chunks.size()) {
                return false;
            }
            finish(job);
            return true;
        });
    }

    private static void requestWindow(ServerLevel level, Job job) {
        int ahead = Math.min(job.chunks.size(), job.cursor + WINDOW);
        for (int i = job.cursor; i < ahead; i++) {
            long packed = job.chunks.getLong(i);
            if (job.ticketed.add(packed)) {
                level.getChunkSource().addRegionTicket(
                        TicketType.FORCED, new ChunkPos(packed), TICKET_RADIUS,
                        new ChunkPos(packed));
            }
        }
    }

    private static void release(ServerLevel level, Job job, ChunkPos chunk) {
        long packed = chunk.toLong();
        if (job.ticketed.remove(packed)) {
            level.getChunkSource().removeRegionTicket(
                    TicketType.FORCED, chunk, TICKET_RADIUS, chunk);
        }
    }

    private static void finish(Job job) {
        releaseAll(job);
        if (job.onDone != null) {
            job.onDone.finished(job.placed, job.removed, job.cleared);
        }
    }

    private static void releaseAll(Job job) {
        for (long packed : job.ticketed) {
            ChunkPos chunk = new ChunkPos(packed);
            job.level.getChunkSource().removeRegionTicket(
                    TicketType.FORCED, chunk, TICKET_RADIUS, chunk);
        }
        job.ticketed.clear();
    }

    public static void abandon(ServerLevel level) {
        JOBS.removeIf(job -> {
            if (job.level != level) {
                return false;
            }
            releaseAll(job);
            return true;
        });
    }

    public static void abandonAll() {
        for (Job job : JOBS) {
            releaseAll(job);
        }
        JOBS.clear();
    }

    private static int clearBlocksIn(ServerLevel level, Job job, ChunkPos chunk) {
        if (job.placements == null || job.placements.length == 0) {
            return 0;
        }
        int removed = 0;
        for (long packed : job.placements) {
            BlockPos pos = BlockPos.of(packed);
            if ((pos.getX() >> 4) != chunk.x || (pos.getZ() >> 4) != chunk.z) {
                continue;
            }
            if (!job.box.isInside(pos) || level.getBlockState(pos).isAir()) {
                continue;
            }
            level.removeBlock(pos, false);
            removed++;
        }
        return removed;
    }

    private static int clearEntitiesIn(ServerLevel level, Job job, ChunkPos chunk) {
        if (job.ownerKey == null) {
            return 0;
        }
        AABB area = new AABB(
                chunk.getMinBlockX(), job.box.minY(), chunk.getMinBlockZ(),
                chunk.getMaxBlockX() + 1, job.box.maxY() + 1, chunk.getMaxBlockZ() + 1);

        for (Entity entity : level.getEntities((Entity) null, area, e -> !(e instanceof Player))) {
            if (job.spawned.contains(entity.getUUID())) {
                continue;
            }
            if (OwnershipStamp.ownedBy(entity, job.ownerKey)
                    && job.box.isInside(entity.blockPosition())) {
                job.doomed.add(entity.getUUID());
            }
        }
        return discardOwned(level, job, area);
    }

    private static int discardOwned(ServerLevel level, Job job, AABB area) {
        List<String> protectedPatterns = ConfigBridge.global().structure_refresh.never_clear;
        boolean anyProtected = protectedPatterns != null && !protectedPatterns.isEmpty();

        List<Entity> doomed = new ArrayList<>();
        for (Entity entity : level.getEntities((Entity) null, area, e -> !(e instanceof Player))) {
            if (!job.doomed.contains(entity.getUUID())) {
                continue;
            }
            if (anyProtected && StructureRefresh.isProtected(entity, protectedPatterns)) {
                continue;
            }
            doomed.add(entity);
        }
        doomed.forEach(entity -> {
            job.doomed.remove(entity.getUUID());
            entity.discard();
        });
        return doomed.size();
    }

    private static BoundingBox clip(ChunkPos chunk, BoundingBox structure) {
        return new BoundingBox(
                Math.max(chunk.getMinBlockX(), structure.minX()),
                structure.minY(),
                Math.max(chunk.getMinBlockZ(), structure.minZ()),
                Math.min(chunk.getMaxBlockX(), structure.maxX()),
                structure.maxY(),
                Math.min(chunk.getMaxBlockZ(), structure.maxZ()));
    }
}
