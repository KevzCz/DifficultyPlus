package net.pixeldreamstudios.difficultyplus.compat.xaero;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
import net.pixeldreamstudios.difficultyplus.compat.ZoneTooltips;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;
import xaero.map.highlight.ChunkHighlighter;

import java.util.List;

public final class WorldMapZoneHighlighter extends ChunkHighlighter {

    private static final String SEPARATOR = " · ";

    private final int[] quartStore = new int[16];

    public WorldMapZoneHighlighter() {
        super(false);
    }

    @Override
    public int calculateRegionHash(ResourceKey<Level> dimension, int regionX, int regionZ) {
        return enabled() ? ZoneChunkCache.regionHash(id(dimension), regionX, regionZ) : 0;
    }

    @Override
    public boolean regionHasHighlights(ResourceKey<Level> dimension, int regionX, int regionZ) {
        return enabled() && ZoneChunkCache.regionHasEntries(id(dimension), regionX, regionZ);
    }

    @Override
    public boolean chunkIsHighlit(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return enabled()
                && ZoneChunkCache.get(id(dimension), ChunkPos.asLong(chunkX, chunkZ)) != null;
    }

    @Override
    protected int[] getColors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        return ZoneColours.colourFor(id(dimension), chunkX, chunkZ, resultStore);
    }

    @Override
    public int[] getChunkHighlitColor(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        if (!enabled()) {
            return super.getChunkHighlitColor(dimension, chunkX, chunkZ);
        }
        if (!ZoneColours.fillQuarts(id(dimension), chunkX, chunkZ, quartStore, this::setResult)) {
            return super.getChunkHighlitColor(dimension, chunkX, chunkZ);
        }
        return resultStore;
    }

    @Override
    public Component getChunkHighlightSubtleTooltip(ResourceKey<Level> dimension,
                                                    int chunkX, int chunkZ) {
        return enabled()
                ? ZoneTooltips.summary(id(dimension), ChunkPos.asLong(chunkX, chunkZ))
                : null;
    }

    @Override
    public Component getBlockHighlightSubtleTooltip(ResourceKey<Level> dimension,
                                                    int blockX, int blockZ) {
        return enabled()
                ? ZoneTooltips.summary(id(dimension), ChunkPos.asLong(blockX >> 4, blockZ >> 4),
                        ZoneTooltips.quartOf(blockX, blockZ))
                : null;
    }

    @Override
    public Component getChunkHighlightBluntTooltip(ResourceKey<Level> dimension,
                                                   int chunkX, int chunkZ) {
        return blunt(dimension, ChunkPos.asLong(chunkX, chunkZ), -1);
    }

    @Override
    public Component getBlockHighlightBluntTooltip(ResourceKey<Level> dimension,
                                                   int blockX, int blockZ) {
        return blunt(dimension, ChunkPos.asLong(blockX >> 4, blockZ >> 4),
                ZoneTooltips.quartOf(blockX, blockZ));
    }

    private Component blunt(ResourceKey<Level> dimension, long chunk, int quart) {
        if (!enabled()) {
            return null;
        }
        List<Component> lines = ZoneTooltips.lines(id(dimension), chunk, quart,
                ClientConfig.get().show_resistances);
        if (lines.isEmpty()) {
            return null;
        }
        MutableComponent result = lines.get(0).copy();
        for (int i = 1; i < lines.size(); i++) {
            result.append(Component.literal(SEPARATOR).withStyle(ChatFormatting.DARK_GRAY))
                    .append(lines.get(i));
        }
        return result;
    }

    @Override
    public void addMinimapBlockHighlightTooltips(List<Component> tooltip,
                                                 ResourceKey<Level> dimension,
                                                 int blockX, int blockZ, int y) {
        if (!enabled()) {
            return;
        }
        tooltip.addAll(ZoneTooltips.lines(id(dimension),
                ChunkPos.asLong(blockX >> 4, blockZ >> 4),
                ZoneTooltips.quartOf(blockX, blockZ),
                ClientConfig.get().show_resistances));
    }

    private static boolean enabled() {
        return ClientConfig.get().show_overlay;
    }

    private static ResourceLocation id(ResourceKey<Level> dimension) {
        return dimension.location();
    }
}
