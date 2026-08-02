package net.pixeldreamstudios.difficultyplus.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
import net.pixeldreamstudios.difficultyplus.internal.LevelGate;
import net.pixeldreamstudios.difficultyplus.network.ZoneInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ZoneTooltips {
    private ZoneTooltips() {}

    public static List<Component> lines(ResourceLocation dimension, long chunk,
                                        boolean includeResistances) {
        return lines(dimension, chunk, -1, includeResistances);
    }

    public static List<Component> lines(ResourceLocation dimension, long chunk, int quart,
                                        boolean includeResistances) {
        List<Component> out = new ArrayList<>();
        ZoneChunkCache.Entry entry = ZoneChunkCache.get(dimension, chunk);
        if (entry == null) {
            return out;
        }
        int zoneId = quart < 0 ? entry.zoneId() : entry.zoneAt(quart);
        int levelValue = quart < 0 ? entry.level() : entry.levelAt(quart);
        ZoneInfo zone = ZoneChunkCache.zone(zoneId);
        if (zone == null) {
            return out;
        }

        out.add(Component.literal(pretty(zone.name()))
                .withStyle(style -> style.withColor(zone.colour()))
                .append(Component.literal(" " + levelValue).withStyle(ChatFormatting.GRAY)));

        if (!includeResistances) {
            return out;
        }
        for (ZoneInfo.Resistance resistance : zone.resistances()) {
            if (!LevelGate.allows(resistance.minLevel(), resistance.maxLevel(), levelValue)) {
                continue;
            }
            out.add(describe(resistance));
        }
        return out;
    }

    public static int quartOf(int blockX, int blockZ) {
        return ((blockZ & 15) >> 2) * 4 + ((blockX & 15) >> 2);
    }

    public static Component summary(ResourceLocation dimension, long chunk) {
        List<Component> lines = lines(dimension, chunk, false);
        return lines.isEmpty() ? null : lines.get(0);
    }

    public static Component summary(ResourceLocation dimension, long chunk, int quart) {
        List<Component> lines = lines(dimension, chunk, quart, false);
        return lines.isEmpty() ? null : lines.get(0);
    }

    public static Component describe(ZoneInfo.Resistance resistance) {
        boolean vulnerable = resistance.isVulnerability();
        StringBuilder value = new StringBuilder();
        value.append(vulnerable ? "takes " : "resists ");

        if (resistance.percent() != 0f) {
            int shown = Math.abs(Math.round(resistance.percent() * 100f));
            if (vulnerable) {
                value.append('+').append(shown).append('%');
            } else if (shown >= 100) {
                value.setLength(0);
                value.append("immune");
            } else {
                value.append(shown).append('%');
            }
        }
        if (resistance.flat() != 0f) {
            if (value.length() > 0 && !value.toString().endsWith(" ")) {
                value.append(", ");
            }
            value.append(String.format(Locale.ROOT, "%s%.1f",
                    vulnerable ? "+" : "-", Math.abs(resistance.flat())));
        }

        return Component.empty()
                .append(name(resistance).withStyle(ChatFormatting.GRAY))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(value.toString())
                        .withStyle(vulnerable ? ChatFormatting.RED : ChatFormatting.GREEN));
    }

    public static MutableComponent name(ZoneInfo.Resistance resistance) {
        return DamageTypeNames.of(resistance.damageId(), resistance.isTag(),
                resistance.displayKey(), resistance.displayText());
    }

    private static String pretty(String zoneName) {
        String raw = zoneName.replace('_', ' ');
        return raw.isEmpty() ? raw : Character.toUpperCase(raw.charAt(0)) + raw.substring(1);
    }
}
