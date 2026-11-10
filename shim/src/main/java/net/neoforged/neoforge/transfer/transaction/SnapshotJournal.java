/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.transfer.transaction;

import java.util.ArrayList;
import net.neoforged.neoforge.shim.ShimServices;
import net.neoforged.neoforge.shim.TransactionBackend;
import org.jspecify.annotations.Nullable;

/**
 * A journal that modifies itself during transactions, saving snapshots of its state in objects of type {@code T} in case it needs to revert
 * to a previous state.
 *
 * <ul>
 * <li>Call {@link #updateSnapshots} right before the state of your subclass is modified in a transaction.</li>
 * <li>Override {@link #createSnapshot} and {@link #revertToSnapshot}.</li>
 * <li>Optionally override {@link #onRootCommit} and {@link #releaseSnapshot}.</li>
 * </ul>
 *
 * <p>Mekanism shim: Same semantics as NeoForge's implementation, but driven by close callbacks of the loader's native transactions.
 */
public abstract class SnapshotJournal<T extends @Nullable Object> {

    private static final TransactionBackend BACKEND = ShimServices.TRANSACTIONS;
    private static final Object NO_SNAPSHOT = new Object();

    private final ArrayList<T> snapshots = new ArrayList<>();
    private int snapshotCount = 0;
    private boolean hasOriginalState = false;
    @Nullable
    private T originalState = null;

    /**
     * {@return a new snapshot of the current state of this journal}
     */
    protected abstract T createSnapshot();

    /**
     * Reverts the state of this journal to the given snapshot.
     */
    protected abstract void revertToSnapshot(T snapshot);

    /**
     * Called when a snapshot is no longer needed, to allow reusing it.
     */
    protected void releaseSnapshot(T snapshot) {}

    /**
     * Called after the root transaction commits if this journal was modified during it.
     *
     * @param originalState The state of this journal before the root transaction was opened.
     */
    protected void onRootCommit(T originalState) {}

    /**
     * {@return {@code true} if this journal has changes in any currently open transaction}
     */
    public final boolean isInTransaction() {
        return snapshotCount > 0;
    }

    /**
     * Takes a snapshot of the current state of this journal if one has not already been taken for the given transaction. Must be called
     * before any modification of the state of this journal.
     */
    @SuppressWarnings("unchecked")
    public void updateSnapshots(TransactionContext transaction) {
        Object handle = ((Transaction) transaction).handle;
        int currentDepth = BACKEND.depth(handle);
        snapshots.ensureCapacity(currentDepth + 1);
        for (int i = snapshots.size(); i <= currentDepth; i++) {
            snapshots.add((T) NO_SNAPSHOT);
        }
        if (snapshots.get(currentDepth) == NO_SNAPSHOT) {
            T snapshot = createSnapshot();
            snapshotCount++;
            snapshots.set(currentDepth, snapshot);
            BACKEND.addCloseCallback(handle, aborted -> onClose(handle, currentDepth, aborted));
        }
    }

    private void onClose(Object handle, int currentDepth, boolean wasAborted) {
        T snapshot = snapshots.remove(currentDepth);
        if (wasAborted) {
            revertToSnapshot(snapshot);
            releaseSnapshot(snapshot);
            snapshotCount--;
        } else if (currentDepth == 0) {
            if (!hasOriginalState) {
                hasOriginalState = true;
                originalState = snapshot;
                BACKEND.addRootCloseCallback(handle, committed -> callOnRootCommit());
            } else {
                releaseSnapshot(snapshot);
            }
            snapshotCount = 0;
        } else if (snapshots.get(currentDepth - 1) == NO_SNAPSHOT) {
            snapshots.set(currentDepth - 1, snapshot);
            Object parent = BACKEND.getOpenTransaction(handle, currentDepth - 1);
            BACKEND.addCloseCallback(parent, aborted -> onClose(parent, currentDepth - 1, aborted));
        } else {
            releaseSnapshot(snapshot);
            snapshotCount--;
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private void callOnRootCommit() {
        T originalState = this.originalState;
        this.originalState = null;
        this.hasOriginalState = false;
        onRootCommit(originalState);
        releaseSnapshot(originalState);
    }
}
