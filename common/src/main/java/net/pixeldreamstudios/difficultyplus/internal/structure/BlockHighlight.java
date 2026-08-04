package net.pixeldreamstudios.difficultyplus.internal.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockHighlight {
    private BlockHighlight() {}

    private static final int GLOW_COLOUR = 0xFFFF3020;

    private static final int MAX_BLOCKS = 64;

    private static final int DURATION_TICKS = 600;

    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

    private record Marker(BlockPos pos, Display.BlockDisplay display) {}

    private record Session(ServerLevel level, List<Marker> markers, int expiresAtTick) {}

    public static void outline(ServerLevel level, ServerPlayer player, List<BlockPos> positions) {
        clear(player);
        if (positions.isEmpty()) {
            return;
        }
        List<BlockPos> capped = positions.size() > MAX_BLOCKS
                ? positions.subList(0, MAX_BLOCKS)
                : positions;

        List<Marker> markers = new ArrayList<>();
        for (BlockPos pos : capped) {
            Display.BlockDisplay display = spawn(level, pos);
            if (display != null) {
                markers.add(new Marker(pos.immutable(), display));
            }
        }
        if (markers.isEmpty()) {
            return;
        }
        ACTIVE.put(player.getUUID(),
                new Session(level, markers, level.getServer().getTickCount() + DURATION_TICKS));
    }

    private static Display.BlockDisplay spawn(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getRenderShape() != RenderShape.MODEL) {
            state = Blocks.RED_STAINED_GLASS.defaultBlockState();
        }

        Display.BlockDisplay marker = EntityType.BLOCK_DISPLAY.create(level);
        if (marker == null) {
            return null;
        }

        marker.moveTo(pos.getX(), pos.getY(), pos.getZ(), 0f, 0f);

        CompoundTag tag = new CompoundTag();
        marker.saveWithoutId(tag);
        tag.put("block_state", NbtUtils.writeBlockState(state));
        tag.putInt("glow_color_override", GLOW_COLOUR);
        tag.put("brightness", brightnessTag());
        tag.putFloat("view_range", 4.0f);
        marker.load(tag);

        marker.setGlowingTag(true);
        marker.setInvulnerable(true);
        marker.setNoGravity(true);
        marker.addTag(StructureOutline.TRANSIENT_TAG);

        if (!level.addFreshEntity(marker)) {
            DifficultyPlus.LOGGER.warn("Difficulty+ highlight: could not spawn marker at {}", pos);
            return null;
        }
        return marker;
    }

    private static CompoundTag brightnessTag() {
        CompoundTag brightness = new CompoundTag();
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        return brightness;
    }

    public static void tick(ServerLevel level) {
        if (ACTIVE.isEmpty()) {
            return;
        }
        int now = level.getServer().getTickCount();
        ACTIVE.entrySet().removeIf(entry -> {
            Session session = entry.getValue();
            if (session.level() != level) {
                return false;
            }
            if (now >= session.expiresAtTick()) {
                session.markers().forEach(marker -> marker.display().discard());
                return true;
            }
            session.markers().removeIf(marker -> {
                if (marker.display().isRemoved()) {
                    return true;
                }
                if (level.getChunkSource().getChunkNow(
                        marker.pos().getX() >> 4, marker.pos().getZ() >> 4) == null) {
                    return false;
                }
                if (level.getBlockState(marker.pos()).isAir()) {
                    marker.display().discard();
                    return true;
                }
                return false;
            });
            return session.markers().isEmpty();
        });
    }

    public static void clear(ServerPlayer player) {
        Session previous = ACTIVE.remove(player.getUUID());
        if (previous != null) {
            previous.markers().forEach(marker -> marker.display().discard());
        }
    }

    public static void clearAll() {
        for (Session session : ACTIVE.values()) {
            session.markers().forEach(marker -> marker.display().discard());
        }
        ACTIVE.clear();
    }
}
