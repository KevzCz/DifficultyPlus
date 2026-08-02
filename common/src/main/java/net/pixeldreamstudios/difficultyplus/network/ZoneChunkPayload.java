package net.pixeldreamstudios.difficultyplus.network;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

public record ZoneChunkPayload(ResourceLocation dimension, LongList chunks,
                               byte[] zoneIds, short[] levels,
                               byte[][] quartZones, short[][] quartLevels)
        implements CustomPacketPayload {

    public static final int QUART_SPAN = 4;

    public static final int QUARTS = QUART_SPAN * QUART_SPAN;

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, "zone_chunks");

    public static final Type<ZoneChunkPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ZoneChunkPayload> STREAM_CODEC =
            StreamCodec.of(ZoneChunkPayload::write, ZoneChunkPayload::read);

    @Override
    public Type<ZoneChunkPayload> type() {
        return TYPE;
    }

    public static void write(RegistryFriendlyByteBuf buf, ZoneChunkPayload payload) {
        buf.writeResourceLocation(payload.dimension());
        int size = payload.chunks().size();
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            buf.writeLong(payload.chunks().getLong(i));
            buf.writeByte(payload.zoneIds()[i]);
            buf.writeShort(payload.levels()[i]);

            byte[] quartZones = payload.quartZones() == null ? null : payload.quartZones()[i];
            short[] quartLevels = payload.quartLevels() == null ? null : payload.quartLevels()[i];
            boolean detailed = quartZones != null && quartLevels != null;
            buf.writeBoolean(detailed);
            if (detailed) {
                for (int q = 0; q < QUARTS; q++) {
                    buf.writeByte(quartZones[q]);
                    buf.writeShort(quartLevels[q]);
                }
            }
        }
    }

    public static ZoneChunkPayload read(RegistryFriendlyByteBuf buf) {
        ResourceLocation dimension = buf.readResourceLocation();
        int size = buf.readVarInt();
        LongList chunks = new LongArrayList(size);
        byte[] zoneIds = new byte[size];
        short[] levels = new short[size];
        byte[][] quartZones = new byte[size][];
        short[][] quartLevels = new short[size][];
        for (int i = 0; i < size; i++) {
            chunks.add(buf.readLong());
            zoneIds[i] = buf.readByte();
            levels[i] = buf.readShort();
            if (buf.readBoolean()) {
                byte[] zones = new byte[QUARTS];
                short[] quarts = new short[QUARTS];
                for (int q = 0; q < QUARTS; q++) {
                    zones[q] = buf.readByte();
                    quarts[q] = buf.readShort();
                }
                quartZones[i] = zones;
                quartLevels[i] = quarts;
            }
        }
        return new ZoneChunkPayload(dimension, chunks, zoneIds, levels, quartZones, quartLevels);
    }
}
