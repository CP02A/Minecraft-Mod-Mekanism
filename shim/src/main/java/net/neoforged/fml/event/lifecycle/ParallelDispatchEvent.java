/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.event.lifecycle;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.neoforged.fml.ModContainer;

/**
 * Mekanism shim: Lifecycle events that FML dispatches in parallel. The loader dispatches them serially, so enqueued work runs immediately.
 */
public abstract class ParallelDispatchEvent extends ModLifecycleEvent {

    public ParallelDispatchEvent(ModContainer container) {
        super(container);
    }

    public CompletableFuture<Void> enqueueWork(Runnable work) {
        work.run();
        return CompletableFuture.completedFuture(null);
    }

    public <T> CompletableFuture<T> enqueueWork(Supplier<T> work) {
        return CompletableFuture.completedFuture(work.get());
    }
}
