package net.pixeldreamstudios.difficultyplus.compat.xaero;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneStyles;
import net.pixeldreamstudios.difficultyplus.config.ClientData;
import net.pixeldreamstudios.difficultyplus.config.LevelColours;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;
import net.pixeldreamstudios.difficultyplus.network.ZoneInfo;

public final class ZoneColours {
    private ZoneColours() {}

    public static final int MAX_SMOOTHING = 3;

    private static final int MAX_LEVEL = 12;
    private static final int LEVEL_BAND = 10;
    private static final int CHUNK_BLOCKS = 16;
    private static final int QUART_SHIFT = 2;
    private static final int QUART_BLOCKS = 1 << QUART_SHIFT;
    private static final int QUART_MASK = QUART_BLOCKS - 1;
    private static final float QUART_CENTRE = QUART_BLOCKS / 2f;
    private static final int CHUNK_QUARTS = CHUNK_BLOCKS >> QUART_SHIFT;
    private static final int CHUNK_QUART_COUNT = CHUNK_QUARTS * CHUNK_QUARTS;
    private static final int WINDOW_CHUNKS = 3;
    private static final int WINDOW = CHUNK_QUARTS * WINDOW_CHUNKS;
    private static final int INNER_START = CHUNK_QUARTS;
    private static final int INNER_END = INNER_START + CHUNK_QUARTS;
    private static final int AROUND = 3;
    private static final int MAJORITY = 5;
    private static final int DASH_MASK = 2;
    private static final long NO_CELL = Long.MIN_VALUE;
    private static final int[][] NEIGHBOURS = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
    private static final int CLEARED_RGB = 0x9AA7B0;
    private static final int SAFE_RGB = 0xE8EEF0;
    private static final int WARN_RGB = 0xE0A040;
    private static final int DANGER_RGB = 0xD9534F;
    private static final int DEADLY_RGB = 0x8E2C5E;
    private static final int UNZONED_RGB = 0xFFFFFF;

    private enum Edge { NONE, STEP, ZONE }

    private enum Severity {
        CLEARED(CLEARED_RGB, 0.35f, 0f), SAFE(SAFE_RGB, 0.5f, 0f), WARN(WARN_RGB, 0.85f, 0.10f), DANGER(DANGER_RGB, 0.9f, 0.16f), DEADLY(DEADLY_RGB, 0.95f, 0.22f);

        private final int rgb;
        private final float edge;
        private final float fill;

        Severity(int rgb, float edge, float fill) {
            this.rgb = rgb;
            this.edge = edge;
            this.fill = fill;
        }
    }

    public static final class Scratch {
        private long[] cells = new long[WINDOW * WINDOW];
        private long[] keys = new long[WINDOW * WINDOW];
        private long[] nextCells = new long[WINDOW * WINDOW];
        private long[] nextKeys = new long[WINDOW * WINDOW];
        private final int[] colours = new int[WINDOW * WINDOW];
        private final Edge[] around = new Edge[AROUND * AROUND];

        private void swap() {
            long[] cellsSpare = cells;
            cells = nextCells;
            nextCells = cellsSpare;
            long[] keysSpare = keys;
            keys = nextKeys;
            nextKeys = keysSpare;
        }
    }

    public static boolean fillQuarts(ResourceLocation dimension, int chunkX, int chunkZ, Scratch scratch, BlockWriter writer, ZoneStyles.Map map) {
        ClientData config = ClientConfig.get();
        ClientData.ColourMode mode = ZoneStyles.mode();
        if (!load(dimension, chunkX, chunkZ, scratch, mode, config)) {
            return false;
        }
        smooth(scratch, Mth.clamp(config.smoothing, 0, MAX_SMOOTHING));
        if (!innerHasZone(scratch)) {
            return false;
        }
        ClientData.OverlayStyle style = ZoneStyles.style(map);
        if (style == ClientData.OverlayStyle.FULL) {
            fillFull(scratch, writer, config);
        } else {
            fillLines(chunkX, chunkZ, scratch, writer, style, mode, config);
        }
        return true;
    }

    private static boolean load(ResourceLocation dimension, int chunkX, int chunkZ, Scratch scratch, ClientData.ColourMode mode, ClientData config) {
        boolean anyZone = false;
        for (int offsetZ = 0; offsetZ < WINDOW_CHUNKS; offsetZ++) {
            for (int offsetX = 0; offsetX < WINDOW_CHUNKS; offsetX++) {
                ZoneChunkCache.Entry entry = ZoneChunkCache.get(dimension, ChunkPos.asLong(chunkX + offsetX - 1, chunkZ + offsetZ - 1));
                int originX = offsetX * CHUNK_QUARTS;
                int originZ = offsetZ * CHUNK_QUARTS;
                for (int quart = 0; quart < CHUNK_QUART_COUNT; quart++) {
                    int index = (originZ + quart / CHUNK_QUARTS) * WINDOW + originX + quart % CHUNK_QUARTS;
                    if (entry == null) {
                        scratch.cells[index] = NO_CELL;
                        scratch.keys[index] = NO_CELL;
                        continue;
                    }
                    int zone = entry.zoneAt(quart);
                    int level = entry.levelAt(quart);
                    scratch.cells[index] = pack(zone, level);
                    scratch.keys[index] = pack(zone, band(level, mode, config));
                    anyZone |= zone != 0;
                }
            }
        }
        return anyZone;
    }

    private static void smooth(Scratch scratch, int passes) {
        for (int pass = 1; pass <= passes; pass++) {
            System.arraycopy(scratch.cells, 0, scratch.nextCells, 0, scratch.cells.length);
            System.arraycopy(scratch.keys, 0, scratch.nextKeys, 0, scratch.keys.length);
            for (int z = pass; z < WINDOW - pass; z++) {
                for (int x = pass; x < WINDOW - pass; x++) {
                    int index = z * WINDOW + x;
                    long key = scratch.keys[index];
                    if (key == NO_CELL) {
                        continue;
                    }
                    int winner = majority(scratch.keys, x, z, key);
                    if (winner >= 0) {
                        scratch.nextCells[index] = scratch.cells[winner];
                        scratch.nextKeys[index] = scratch.keys[winner];
                    }
                }
            }
            scratch.swap();
        }
    }

    private static int majority(long[] keys, int x, int z, long own) {
        for (int[] candidate : NEIGHBOURS) {
            int candidateIndex = (z + candidate[1]) * WINDOW + x + candidate[0];
            long key = keys[candidateIndex];
            if (key == NO_CELL || key == own) {
                continue;
            }
            int count = 0;
            for (int[] other : NEIGHBOURS) {
                if (keys[(z + other[1]) * WINDOW + x + other[0]] == key) {
                    count++;
                }
            }
            if (count >= MAJORITY) {
                return candidateIndex;
            }
        }
        return -1;
    }

    private static boolean innerHasZone(Scratch scratch) {
        for (int z = INNER_START; z < INNER_END; z++) {
            for (int x = INNER_START; x < INNER_END; x++) {
                long cell = scratch.cells[z * WINDOW + x];
                if (cell != NO_CELL && zoneOf(cell) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void fillFull(Scratch scratch, BlockWriter writer, ClientData config) {
        for (int z = INNER_START - 1; z <= INNER_END; z++) {
            for (int x = INNER_START - 1; x <= INNER_END; x++) {
                int index = z * WINDOW + x;
                long cell = scratch.cells[index];
                scratch.colours[index] = cell == NO_CELL ? 0 : argb(zoneOf(cell), levelOf(cell));
            }
        }
        int[] colours = scratch.colours;
        if (!config.smooth_borders) {
            for (int x = 0; x < CHUNK_BLOCKS; x++) {
                for (int z = 0; z < CHUNK_BLOCKS; z++) {
                    writer.set(x, z, colours[(INNER_START + (z >> QUART_SHIFT)) * WINDOW + INNER_START + (x >> QUART_SHIFT)]);
                }
            }
            return;
        }
        for (int x = 0; x < CHUNK_BLOCKS; x++) {
            for (int z = 0; z < CHUNK_BLOCKS; z++) {
                float sampleX = (x - QUART_CENTRE) / QUART_BLOCKS;
                float sampleZ = (z - QUART_CENTRE) / QUART_BLOCKS;
                int cellX = Mth.floor(sampleX);
                int cellZ = Mth.floor(sampleZ);
                float fx = sampleX - cellX;
                float fz = sampleZ - cellZ;
                int top = (INNER_START + cellZ) * WINDOW + INNER_START + cellX;
                int bottom = top + WINDOW;
                writer.set(x, z, bilinear(colours[top], colours[top + 1], colours[bottom], colours[bottom + 1], fx, fz));
            }
        }
    }

    private static void fillLines(int chunkX, int chunkZ, Scratch scratch, BlockWriter writer, ClientData.OverlayStyle style, ClientData.ColourMode mode, ClientData config) {
        int baseX = chunkX * CHUNK_BLOCKS;
        int baseZ = chunkZ * CHUNK_BLOCKS;
        for (int quartZ = 0; quartZ < CHUNK_QUARTS; quartZ++) {
            for (int quartX = 0; quartX < CHUNK_QUARTS; quartX++) {
                int windowX = INNER_START + quartX;
                int windowZ = INNER_START + quartZ;
                int index = windowZ * WINDOW + windowX;
                long here = scratch.cells[index];
                int startX = quartX << QUART_SHIFT;
                int startZ = quartZ << QUART_SHIFT;
                if (here == NO_CELL || zoneOf(here) == 0) {
                    clearQuart(writer, startX, startZ);
                    continue;
                }
                surroundings(scratch, windowX, windowZ, here, scratch.keys[index]);
                int level = levelOf(here);
                Severity severity = mode == ClientData.ColourMode.RELATIVE ? severity(level, config) : null;
                int rgb = severity != null ? severity.rgb : mode == ClientData.ColourMode.LEVEL ? LevelColours.rgbAt(config.level_colours, level) : zoneRgb(zoneOf(here), level);
                float strength = ZoneStyles.strength();
                float edgeAlpha = (severity != null ? severity.edge : config.border_opacity) * strength;
                float stepAlpha = (severity != null ? Math.min(severity.edge, config.step_opacity) : config.step_opacity) * strength;
                float fillAlpha = (style != ClientData.OverlayStyle.WASH ? 0f : severity != null ? severity.fill : config.wash_opacity) * strength;
                for (int blockZ = 0; blockZ < QUART_BLOCKS; blockZ++) {
                    for (int blockX = 0; blockX < QUART_BLOCKS; blockX++) {
                        int x = startX + blockX;
                        int z = startZ + blockZ;
                        float alpha = switch (edgeAt(scratch.around, blockX, blockZ)) {
                            case ZONE -> edgeAlpha;
                            case STEP -> ((baseX + x + baseZ + z) & DASH_MASK) != 0 ? stepAlpha : fillAlpha;
                            case NONE -> fillAlpha;
                        };
                        writer.set(x, z, alpha <= 0f ? 0 : withAlpha(rgb, (int) (Mth.clamp(alpha, 0f, 1f) * 255f)));
                    }
                }
            }
        }
    }

    private static void clearQuart(BlockWriter writer, int startX, int startZ) {
        for (int z = startZ; z < startZ + QUART_BLOCKS; z++) {
            for (int x = startX; x < startX + QUART_BLOCKS; x++) {
                writer.set(x, z, 0);
            }
        }
    }

    private static void surroundings(Scratch scratch, int windowX, int windowZ, long here, long hereKey) {
        for (int[] offset : NEIGHBOURS) {
            int index = (windowZ + offset[1]) * WINDOW + windowX + offset[0];
            long other = scratch.cells[index];
            Edge edge = Edge.NONE;
            if (other != NO_CELL) {
                if (zoneOf(other) != zoneOf(here)) {
                    edge = Edge.ZONE;
                } else if (scratch.keys[index] != hereKey) {
                    edge = Edge.STEP;
                }
            }
            scratch.around[side(offset[0], offset[1])] = edge;
        }
    }

    private static Edge edgeAt(Edge[] around, int blockX, int blockZ) {
        int sideX = blockX == 0 ? -1 : blockX == QUART_MASK ? 1 : 0;
        int sideZ = blockZ == 0 ? -1 : blockZ == QUART_MASK ? 1 : 0;
        Edge edge = Edge.NONE;
        if (sideX != 0) {
            edge = stronger(edge, around[side(sideX, 0)]);
        }
        if (sideZ != 0) {
            edge = stronger(edge, around[side(0, sideZ)]);
        }
        if (sideX != 0 && sideZ != 0) {
            edge = stronger(edge, around[side(sideX, sideZ)]);
        }
        return edge;
    }

    private static int side(int offsetX, int offsetZ) {
        return (offsetZ + 1) * AROUND + offsetX + 1;
    }

    private static Edge stronger(Edge current, Edge other) {
        return other.ordinal() > current.ordinal() ? other : current;
    }

    private static int band(int level, ClientData.ColourMode mode, ClientData config) {
        return mode == ClientData.ColourMode.RELATIVE ? severity(level, config).ordinal() : Math.floorDiv(level, LEVEL_BAND);
    }

    private static Severity severity(int level, ClientData config) {
        int gap = level - ZoneStyles.playerLevel().orElse(level);
        if (gap < -config.relative_cleared_gap) {
            return Severity.CLEARED;
        }
        if (gap <= config.relative_safe_gap) {
            return Severity.SAFE;
        }
        if (gap <= config.relative_warn_gap) {
            return Severity.WARN;
        }
        return gap <= config.relative_danger_gap ? Severity.DANGER : Severity.DEADLY;
    }

    private static long pack(int zone, int value) {
        return ((long) zone << Integer.SIZE) | (value & 0xFFFFFFFFL);
    }

    private static int zoneOf(long cell) {
        return (int) (cell >>> Integer.SIZE);
    }

    private static int levelOf(long cell) {
        return (int) cell;
    }

    private static int bilinear(int c00, int c10, int c01, int c11, float fx, float fz) {
        int top = mix(c00, c10, fx);
        int bottom = mix(c01, c11, fx);
        return mix(top, bottom, fz);
    }

    private static int mix(int left, int right, float t) {
        if (left == right) {
            return left;
        }
        if (left == 0) {
            return withAlpha(right, (int) (((right >>> 24) & 0xFF) * t));
        }
        if (right == 0) {
            return withAlpha(left, (int) (((left >>> 24) & 0xFF) * (1f - t)));
        }
        int alpha = lerp((left >>> 24) & 0xFF, (right >>> 24) & 0xFF, t);
        int red = lerp((left >> 16) & 0xFF, (right >> 16) & 0xFF, t);
        int green = lerp((left >> 8) & 0xFF, (right >> 8) & 0xFF, t);
        int blue = lerp(left & 0xFF, right & 0xFF, t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int lerp(int from, int to, float t) {
        return Mth.clamp((int) (from + (to - from) * t), 0, 255);
    }

    private static int withAlpha(int argb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (argb & 0x00FFFFFF);
    }

    @FunctionalInterface
    public interface BlockWriter {
        void set(int x, int z, int argb);
    }

    private static int argb(int zoneId, int entryLevel) {
        if (zoneId == 0) {
            return 0;
        }
        ClientData config = ClientConfig.get();
        int alpha = (int) (Mth.clamp(config.opacity, 0f, 1f) * ZoneStyles.strength() * 255f);
        return (alpha << 24) | rgb(zoneId, entryLevel);
    }

    public static int rgb(int zoneId, int entryLevel) {
        ClientData config = ClientConfig.get();
        ClientData.ColourMode mode = ZoneStyles.mode();
        if (mode == ClientData.ColourMode.RELATIVE) {
            return severity(entryLevel, config).rgb;
        }
        if (mode == ClientData.ColourMode.LEVEL) {
            return LevelColours.rgbAt(config.level_colours, entryLevel);
        }
        return zoneRgb(zoneId, entryLevel);
    }

    private static int zoneRgb(int zoneId, int entryLevel) {
        ZoneInfo zone = ZoneChunkCache.zone(zoneId);
        int base = zone == null ? UNZONED_RGB : zone.colour();
        float shade = Mth.clamp(entryLevel / (float) MAX_LEVEL, 0f, 1f);
        float scale = 1.0f - shade * 0.45f;
        int red = (int) (((base >> 16) & 0xFF) * scale);
        int green = (int) (((base >> 8) & 0xFF) * scale);
        int blue = (int) ((base & 0xFF) * scale);
        return (red << 16) | (green << 8) | blue;
    }
}
