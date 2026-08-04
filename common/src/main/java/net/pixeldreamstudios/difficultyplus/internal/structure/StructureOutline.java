package net.pixeldreamstudios.difficultyplus.internal.structure;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.pixeldreamstudios.difficultyplus.item.GemKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StructureOutline {
    private StructureOutline() {}

    private static final double EDGE_THICKNESS = 0.25;

    private static final double SEGMENT = 24.0;

    public static final String TRANSIENT_TAG = "difficultyplus_marker";

    private static final Map<UUID, Outline> ACTIVE = new ConcurrentHashMap<>();

    private static final class Outline {
        StructureStart target;
        int colour;
        boolean locked;
        final List<Display.BlockDisplay> markers = new ArrayList<>();
    }

    public static void show(ServerPlayer player, ServerLevel level, StructureStart target,
                            int colour) {
        Outline existing = ACTIVE.get(player.getUUID());
        if (existing != null && existing.locked) {
            return;
        }
        if (existing != null && existing.target == target) {
            if (existing.colour != colour) {
                existing.colour = colour;
                recolour(existing, colour);
            }
            return;
        }
        clear(player);

        Outline outline = new Outline();
        outline.target = target;
        outline.colour = colour;
        buildEdges(level, actualBox(target), colour, outline.markers);

        if (!outline.markers.isEmpty()) {
            ACTIVE.put(player.getUUID(), outline);
        }
    }

    public static BoundingBox actualBox(StructureStart start) {
        BoundingBox box = start.getBoundingBox();
        Integer shift = null;

        for (StructurePiece piece : start.getPieces()) {
            if (!(piece instanceof TemplateStructurePiece template)) {
                return box;
            }
            int drift = template.templatePosition().getY() - piece.getBoundingBox().minY();
            if (shift == null) {
                shift = drift;
            } else if (shift != drift) {
                return box;
            }
        }
        if (shift == null || shift == 0) {
            return box;
        }
        return box.moved(0, shift, 0);
    }

    private static void buildEdges(ServerLevel level, BoundingBox box, int colour,
                                   List<Display.BlockDisplay> into) {
        double minX = box.minX();
        double minY = box.minY();
        double minZ = box.minZ();
        double spanX = box.getXSpan();
        double spanY = box.getYSpan();
        double spanZ = box.getZSpan();
        double maxX = minX + spanX;
        double maxY = minY + spanY;
        double maxZ = minZ + spanZ;

        for (double y : new double[]{minY, maxY}) {
            for (double z : new double[]{minZ, maxZ}) {
                add(into, level, minX, y, z, spanX, EDGE_THICKNESS, EDGE_THICKNESS, colour);
            }
        }
        for (double x : new double[]{minX, maxX}) {
            for (double z : new double[]{minZ, maxZ}) {
                add(into, level, x, minY, z, EDGE_THICKNESS, spanY, EDGE_THICKNESS, colour);
            }
        }
        for (double x : new double[]{minX, maxX}) {
            for (double y : new double[]{minY, maxY}) {
                add(into, level, x, y, minZ, EDGE_THICKNESS, EDGE_THICKNESS, spanZ, colour);
            }
        }
    }

    private static void add(List<Display.BlockDisplay> into, ServerLevel level,
                            double x, double y, double z,
                            double scaleX, double scaleY, double scaleZ, int colour) {
        double length = Math.max(scaleX, Math.max(scaleY, scaleZ));
        if (length > SEGMENT) {
            int pieces = (int) Math.ceil(length / SEGMENT);
            double step = length / pieces;
            boolean alongX = scaleX >= scaleY && scaleX >= scaleZ;
            boolean alongY = scaleY >= scaleX && scaleY >= scaleZ;

            for (int i = 0; i < pieces; i++) {
                double offset = i * step;
                add(into, level,
                        alongX ? x + offset : x,
                        alongY ? y + offset : y,
                        (!alongX && !alongY) ? z + offset : z,
                        alongX ? step : scaleX,
                        alongY ? step : scaleY,
                        (!alongX && !alongY) ? step : scaleZ,
                        colour);
            }
            return;
        }

        Display.BlockDisplay marker = EntityType.BLOCK_DISPLAY.create(level);
        if (marker == null) {
            return;
        }
        marker.moveTo(x, y, z, 0f, 0f);

        CompoundTag tag = new CompoundTag();
        marker.saveWithoutId(tag);
        tag.put("block_state", NbtUtils.writeBlockState(Blocks.WHITE_CONCRETE.defaultBlockState()));
        tag.putInt("glow_color_override", colour);
        tag.putFloat("view_range", 4.0f);
        tag.put("brightness", brightness());
        tag.put("transformation", scale((float) scaleX, (float) scaleY, (float) scaleZ));
        marker.load(tag);
        marker.setGlowingTag(true);
        marker.addTag(TRANSIENT_TAG);

        if (level.addFreshEntity(marker)) {
            into.add(marker);
        }
    }

    public static void lock(ServerPlayer player, ServerLevel level, StructureStart target,
                            int colour) {
        Outline outline = ACTIVE.get(player.getUUID());
        if (outline == null || outline.target != target) {
            clear(player);
            show(player, level, target, colour);
            outline = ACTIVE.get(player.getUUID());
            if (outline == null) {
                return;
            }
        }
        outline.locked = true;
        outline.colour = colour;
        recolour(outline, colour);
    }

    public static void unlock(ServerPlayer player) {
        Outline outline = ACTIVE.get(player.getUUID());
        if (outline == null) {
            return;
        }
        outline.locked = false;
        outline.colour = GemKind.SURVEY_COLOUR;
        recolour(outline, GemKind.SURVEY_COLOUR);
    }

    public static void clear(ServerPlayer player) {
        Outline outline = ACTIVE.remove(player.getUUID());
        if (outline != null) {
            outline.markers.forEach(Display.BlockDisplay::discard);
        }
    }

    public static void clearAll() {
        for (Outline outline : ACTIVE.values()) {
            outline.markers.forEach(Display.BlockDisplay::discard);
        }
        ACTIVE.clear();
    }

    public static boolean isLocked(ServerPlayer player) {
        Outline outline = ACTIVE.get(player.getUUID());
        return outline != null && outline.locked;
    }

    public static BoundingBox targetBox(ServerPlayer player) {
        Outline outline = ACTIVE.get(player.getUUID());
        return outline == null ? null : outline.target.getBoundingBox();
    }

    private static void recolour(Outline outline, int colour) {
        for (Display.BlockDisplay marker : outline.markers) {
            CompoundTag tag = new CompoundTag();
            marker.saveWithoutId(tag);
            tag.putInt("glow_color_override", colour);
            marker.load(tag);
        }
    }

    private static CompoundTag brightness() {
        CompoundTag brightness = new CompoundTag();
        brightness.putInt("block", 15);
        brightness.putInt("sky", 15);
        return brightness;
    }

    private static CompoundTag scale(float x, float y, float z) {
        CompoundTag transformation = new CompoundTag();
        transformation.put("translation", floats(0f, 0f, 0f));
        transformation.put("scale", floats(x, y, z));
        transformation.put("left_rotation", floats(0f, 0f, 0f, 1f));
        transformation.put("right_rotation", floats(0f, 0f, 0f, 1f));
        return transformation;
    }

    private static ListTag floats(float... values) {
        ListTag list = new ListTag();
        for (float value : values) {
            list.add(FloatTag.valueOf(value));
        }
        return list;
    }
}
