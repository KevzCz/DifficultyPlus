package net.pixeldreamstudios.difficultyplus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.DifficultyPlus;

public record PlayerLevelPayload(int level) implements CustomPacketPayload {

    public static final int UNKNOWN = -1;

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(DifficultyPlus.MOD_ID, "player_level");

    public static final Type<PlayerLevelPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerLevelPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, PlayerLevelPayload::level, PlayerLevelPayload::new);

    @Override
    public Type<PlayerLevelPayload> type() {
        return TYPE;
    }
}
