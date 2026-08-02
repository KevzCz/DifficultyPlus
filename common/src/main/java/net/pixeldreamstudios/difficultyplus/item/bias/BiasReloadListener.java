package net.pixeldreamstudios.difficultyplus.item.bias;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class BiasReloadListener implements PreparableReloadListener {

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                          ProfilerFiller prepareProfiler,
                                          ProfilerFiller applyProfiler,
                                          Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> manager, prepareExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(BiasTables::reload, applyExecutor);
    }
}
