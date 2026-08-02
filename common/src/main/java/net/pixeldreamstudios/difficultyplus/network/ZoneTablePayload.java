package net.pixeldreamstudios.difficultyplus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

import java.util.ArrayList;
import java.util.List;

public record ZoneTablePayload(int configHash, List<ZoneInfo> zones)
        implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, "zone_table");

    public static final Type<ZoneTablePayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, ZoneTablePayload> STREAM_CODEC =
            StreamCodec.of(ZoneTablePayload::write, ZoneTablePayload::read);

    @Override
    public Type<ZoneTablePayload> type() {
        return TYPE;
    }

    public static void write(RegistryFriendlyByteBuf buf, ZoneTablePayload payload) {
        buf.writeInt(payload.configHash());
        buf.writeVarInt(payload.zones().size());
        for (ZoneInfo zone : payload.zones()) {
            ZoneInfo.write(buf, zone);
        }
    }

    public static ZoneTablePayload read(RegistryFriendlyByteBuf buf) {
        int hash = buf.readInt();
        int count = buf.readVarInt();
        List<ZoneInfo> zones = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            zones.add(ZoneInfo.read(buf));
        }
        return new ZoneTablePayload(hash, List.copyOf(zones));
    }
}
