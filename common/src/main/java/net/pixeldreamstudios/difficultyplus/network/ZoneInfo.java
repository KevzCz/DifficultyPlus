package net.pixeldreamstudios.difficultyplus.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record ZoneInfo(int id, String name, int colour, String label, List<Resistance> resistances) {

    public record Resistance(ResourceLocation damageId, boolean isTag, float percent, float flat,
                             String displayKey, String displayText,
                             int minLevel, int maxLevel) {

        public boolean isVulnerability() {
            return percent < 0f || flat < 0f;
        }

        public static void write(RegistryFriendlyByteBuf buf, Resistance value) {
            buf.writeResourceLocation(value.damageId());
            buf.writeBoolean(value.isTag());
            buf.writeFloat(value.percent());
            buf.writeFloat(value.flat());
            buf.writeUtf(value.displayKey() == null ? "" : value.displayKey());
            buf.writeUtf(value.displayText() == null ? "" : value.displayText());
            buf.writeVarInt(value.minLevel());
            buf.writeVarInt(value.maxLevel());
        }

        public static Resistance read(RegistryFriendlyByteBuf buf) {
            ResourceLocation id = buf.readResourceLocation();
            boolean tag = buf.readBoolean();
            float percent = buf.readFloat();
            float flat = buf.readFloat();
            String key = buf.readUtf();
            String text = buf.readUtf();
            int minLevel = buf.readVarInt();
            int maxLevel = buf.readVarInt();
            return new Resistance(id, tag, percent, flat,
                    key.isEmpty() ? null : key, text.isEmpty() ? null : text,
                    minLevel, maxLevel);
        }
    }

    public static void write(RegistryFriendlyByteBuf buf, ZoneInfo value) {
        buf.writeVarInt(value.id());
        buf.writeUtf(value.name());
        buf.writeInt(value.colour());
        buf.writeUtf(value.label() == null ? "" : value.label());
        buf.writeVarInt(value.resistances().size());
        for (Resistance resistance : value.resistances()) {
            Resistance.write(buf, resistance);
        }
    }

    public static ZoneInfo read(RegistryFriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String name = buf.readUtf();
        int colour = buf.readInt();
        String label = buf.readUtf();
        int count = buf.readVarInt();
        List<Resistance> resistances = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            resistances.add(Resistance.read(buf));
        }
        return new ZoneInfo(id, name, colour, label.isEmpty() ? null : label, List.copyOf(resistances));
    }
}
