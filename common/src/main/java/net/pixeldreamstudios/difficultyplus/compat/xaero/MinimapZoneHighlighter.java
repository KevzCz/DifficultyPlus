package net.pixeldreamstudios.difficultyplus.compat.xaero;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.pixeldreamstudios.difficultyplus.client.zone.ZoneChunkCache;
import net.pixeldreamstudios.difficultyplus.compat.ZoneTooltips;
import net.pixeldreamstudios.difficultyplus.internal.ClientConfig;
import xaero.common.minimap.highlight.ChunkHighlighter;
import xaero.hud.minimap.info.render.compile.InfoDisplayCompiler;

import java.util.List;

public final class MinimapZoneHighlighter extends ChunkHighlighter {

    private final int[] quartStore = new int[16];

    public MinimapZoneHighlighter() {
        super(false);
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
    public void addChunkHighlightTooltips(InfoDisplayCompiler compiler,
                                          ResourceKey<Level> dimension,
                                          int chunkX, int chunkZ, int y) {
        emit(compiler, dimension, ChunkPos.asLong(chunkX, chunkZ), -1);
    }

    @Override
    public void addBlockHighlightTooltips(InfoDisplayCompiler compiler,
                                          ResourceKey<Level> dimension,
                                          int blockX, int blockZ, int y) {
        emit(compiler, dimension, ChunkPos.asLong(blockX >> 4, blockZ >> 4),
                ZoneTooltips.quartOf(blockX, blockZ));
    }

    private void emit(InfoDisplayCompiler compiler, ResourceKey<Level> dimension,
                      long chunk, int quart) {
        if (!enabled()) {
            return;
        }
        List<Component> lines = ZoneTooltips.lines(id(dimension), chunk, quart,
                ClientConfig.get().show_resistances);
        for (Component line : lines) {
            compiler.addLine(line);
        }
    }

    private static boolean enabled() {
        return ClientConfig.get().show_overlay;
    }

    private static ResourceLocation id(ResourceKey<Level> dimension) {
        return dimension.location();
    }
}
