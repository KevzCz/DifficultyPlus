package net.pixeldreamstudios.difficultyplus.client.zone;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;
import net.pixeldreamstudios.difficultyplus.network.ZoneChunkPayload;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class ZoneStore {
    private ZoneStore() {}

    private static final int MAGIC = 0x445A4E31;

    private static final int FORMAT = 2;

    public static Long2ObjectMap<ZoneChunkCache.Entry> read(String worldId,
                                                            ResourceLocation dimension,
                                                            int configHash) {
        Path file = fileFor(worldId, dimension);
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try (DataInputStream in = new DataInputStream(Files.newInputStream(file))) {
            if (in.readInt() != MAGIC || in.readInt() != FORMAT) {
                return null;
            }
            if (in.readInt() != configHash) {
                Files.deleteIfExists(file);
                return null;
            }
            int count = in.readInt();
            Long2ObjectMap<ZoneChunkCache.Entry> out = new Long2ObjectOpenHashMap<>(count);
            for (int i = 0; i < count; i++) {
                long chunk = in.readLong();
                int zoneId = in.readByte() & 0xFF;
                int level = in.readShort();
                byte[] quartZones = null;
                short[] quartLevels = null;
                if (in.readBoolean()) {
                    quartZones = new byte[ZoneChunkPayload.QUARTS];
                    quartLevels = new short[ZoneChunkPayload.QUARTS];
                    for (int q = 0; q < ZoneChunkPayload.QUARTS; q++) {
                        quartZones[q] = in.readByte();
                        quartLevels[q] = in.readShort();
                    }
                }
                out.put(chunk, new ZoneChunkCache.Entry(zoneId, level, quartZones, quartLevels));
            }
            return out;
        } catch (IOException e) {
            DifficultyPlus.LOGGER.warn("Difficulty+ could not read zone store {}", file, e);
            return null;
        }
    }

    public static void write(String worldId, ResourceLocation dimension, int configHash,
                             Long2ObjectMap<ZoneChunkCache.Entry> entries) {
        Path file = fileFor(worldId, dimension);
        if (file == null || entries == null || entries.isEmpty()) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
                out.writeInt(MAGIC);
                out.writeInt(FORMAT);
                out.writeInt(configHash);
                out.writeInt(entries.size());
                for (Long2ObjectMap.Entry<ZoneChunkCache.Entry> entry
                        : entries.long2ObjectEntrySet()) {
                    ZoneChunkCache.Entry value = entry.getValue();
                    out.writeLong(entry.getLongKey());
                    out.writeByte(value.zoneId());
                    out.writeShort(value.level());
                    out.writeBoolean(value.hasDetail());
                    if (value.hasDetail()) {
                        for (int q = 0; q < ZoneChunkPayload.QUARTS; q++) {
                            out.writeByte(value.quartZones()[q]);
                            out.writeShort(value.quartLevels()[q]);
                        }
                    }
                }
            }
        } catch (IOException e) {
            DifficultyPlus.LOGGER.warn("Difficulty+ could not write zone store {}", file, e);
        }
    }

    public static void wipe(String worldId) {
        Path dir = dirFor(worldId);
        if (dir == null || !Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException e) {
            DifficultyPlus.LOGGER.warn("Difficulty+ could not wipe zone store {}", dir, e);
        }
    }

    private static Path dirFor(String worldId) {
        if (worldId == null || worldId.isBlank()) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return null;
        }
        return client.gameDirectory.toPath()
                .resolve("difficultyplus")
                .resolve("zones")
                .resolve(sanitise(worldId));
    }

    private static Path fileFor(String worldId, ResourceLocation dimension) {
        Path dir = dirFor(worldId);
        return dir == null ? null : dir.resolve(sanitise(dimension.toString()) + ".bin");
    }

    private static String sanitise(String raw) {
        return raw.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
