package net.pixeldreamstudios.difficultyplus.internal.structure;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.utils.value.IntValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.pixeldreamstudios.difficultyplus.internal.level.StructureLevelAccess;
import net.pixeldreamstudios.difficultyplus.internal.zone.StructureLookup;

import java.util.Arrays;
import java.util.Optional;

public final class PlacementTracker {
    private PlacementTracker() {}

    public static void register() {
        BlockEvent.PLACE.register(PlacementTracker::onPlace);
        BlockEvent.BREAK.register(PlacementTracker::onBreak);
    }

    private static EventResult onPlace(Level level, BlockPos pos, BlockState state, Entity placer) {
        if (placer instanceof Player && level instanceof ServerLevel serverLevel) {
            record(serverLevel, pos, true);
        }
        return EventResult.pass();
    }

    private static EventResult onBreak(Level level, BlockPos pos, BlockState state,
                                       ServerPlayer breaker, IntValue experience) {
        if (level instanceof ServerLevel serverLevel) {
            record(serverLevel, pos, false);
        }
        return EventResult.pass();
    }

    private static void record(ServerLevel level, BlockPos pos, boolean add) {
        Optional<StructureStart> start = StructureLookup.startAt(level, pos);
        if (start.isEmpty()) {
            return;
        }
        StructureLevelAccess access = (StructureLevelAccess) (Object) start.get();
        long packed = pos.asLong();
        long[] current = access.difficultyplus$placements();

        if (add) {
            if (contains(current, packed)) {
                return;
            }
            long[] next = current == null ? new long[1] : Arrays.copyOf(current, current.length + 1);
            next[next.length - 1] = packed;
            access.difficultyplus$setPlacements(next);
            return;
        }

        if (!contains(current, packed)) {
            return;
        }
        long[] next = new long[current.length - 1];
        int cursor = 0;
        for (long value : current) {
            if (value != packed) {
                next[cursor++] = value;
            }
        }
        access.difficultyplus$setPlacements(next);
    }

    private static boolean contains(long[] values, long target) {
        if (values == null) {
            return false;
        }
        for (long value : values) {
            if (value == target) {
                return true;
            }
        }
        return false;
    }

    public static long[] placementsOf(StructureStart start) {
        long[] stored = ((StructureLevelAccess) (Object) start).difficultyplus$placements();
        return stored == null ? new long[0] : stored;
    }
}
