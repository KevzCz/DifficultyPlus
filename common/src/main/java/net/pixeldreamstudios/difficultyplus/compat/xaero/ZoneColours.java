package net.pixeldreamstudios.difficultyplus.compat.xaero;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
import net.pixeldreamstudios.difficultyplus.config.ClientData;
import net.pixeldreamstudios.difficultyplus.config.LevelColours;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;
import net.pixeldreamstudios.difficultyplus.network.ZoneInfo;

import java.util.Arrays;

public final class ZoneColours {
    private ZoneColours() {}

    private static final int MAX_LEVEL = 12;


    public static int[] colourFor(ResourceLocation dimension, int chunkX, int chunkZ, int[] store) {
        if (!ClientConfig.get().show_overlay) {
            return null;
        }
        int own = argbAt(dimension, chunkX, chunkZ);
        if (own == 0) {
            return null;
        }
        store[0] = own;
        store[1] = argbAt(dimension, chunkX, chunkZ - 1);
        store[2] = argbAt(dimension, chunkX + 1, chunkZ);
        store[3] = argbAt(dimension, chunkX, chunkZ + 1);
        store[4] = argbAt(dimension, chunkX - 1, chunkZ);
        return store;
    }


    public static int[] quartColours(ResourceLocation dimension, int chunkX, int chunkZ,
                                     int[] store) {
        ZoneChunkCache.Entry entry =
                ZoneChunkCache.get(dimension, ChunkPos.asLong(chunkX, chunkZ));
        if (entry == null) {
            Arrays.fill(store, 0);
            return store;
        }
        for (int q = 0; q < store.length; q++) {
            store[q] = argb(entry.zoneAt(q), entry.levelAt(q));
        }
        return store;
    }

    public static boolean fillQuarts(ResourceLocation dimension, int chunkX, int chunkZ,
                                     int[] store, BlockWriter writer) {
        if (argbAt(dimension, chunkX, chunkZ) == 0) {
            return false;
        }
        quartColours(dimension, chunkX, chunkZ, store);

        if (!ClientConfig.get().smooth_borders) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    writer.set(x, z, store[(z >> 2) * 4 + (x >> 2)]);
                }
            }
            return true;
        }

        int baseQuartX = chunkX << 2;
        int baseQuartZ = chunkZ << 2;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                float sampleX = (x - 2f) / 4f;
                float sampleZ = (z - 2f) / 4f;
                int cellX = Mth.floor(sampleX);
                int cellZ = Mth.floor(sampleZ);
                float fx = sampleX - cellX;
                float fz = sampleZ - cellZ;

                int c00 = quartColour(dimension, baseQuartX + cellX, baseQuartZ + cellZ);
                int c10 = quartColour(dimension, baseQuartX + cellX + 1, baseQuartZ + cellZ);
                int c01 = quartColour(dimension, baseQuartX + cellX, baseQuartZ + cellZ + 1);
                int c11 = quartColour(dimension, baseQuartX + cellX + 1, baseQuartZ + cellZ + 1);

                writer.set(x, z, bilinear(c00, c10, c01, c11, fx, fz));
            }
        }
        return true;
    }

    private static int quartColour(ResourceLocation dimension, int quartX, int quartZ) {
        ZoneChunkCache.Entry entry =
                ZoneChunkCache.get(dimension, ChunkPos.asLong(quartX >> 2, quartZ >> 2));
        if (entry == null) {
            return 0;
        }
        int index = (quartZ & 3) * 4 + (quartX & 3);
        return argb(entry.zoneAt(index), entry.levelAt(index));
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

    private static int argbAt(ResourceLocation dimension, int chunkX, int chunkZ) {
        ZoneChunkCache.Entry entry =
                ZoneChunkCache.get(dimension, ChunkPos.asLong(chunkX, chunkZ));
        if (entry == null) {
            return 0;
        }
        return argb(entry.zoneId(), entry.level());
    }

    private static int argb(int zoneId, int entryLevel) {
        if (zoneId == 0) {
            return 0;
        }
        ClientData config = ClientConfig.get();
        int alpha = (int) (Mth.clamp(config.opacity, 0f, 1f) * 255f);

        if (config.colour_by_level) {
            int ramp = LevelColours.rgbAt(config.level_colours, entryLevel);
            return (alpha << 24) | ramp;
        }

        ZoneInfo zone = ZoneChunkCache.zone(zoneId);
        int base = zone == null ? 0xFFFFFF : zone.colour();

        float shade = Mth.clamp(entryLevel / (float) MAX_LEVEL, 0f, 1f);
        float scale = 1.0f - shade * 0.45f;

        int red = (int) (((base >> 16) & 0xFF) * scale);
        int green = (int) (((base >> 8) & 0xFF) * scale);
        int blue = (int) ((base & 0xFF) * scale);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
}
