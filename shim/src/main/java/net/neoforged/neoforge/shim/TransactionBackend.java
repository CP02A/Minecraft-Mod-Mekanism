/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim;

import java.util.function.Consumer;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Backend for the shimmed NeoForge transaction API. Implemented by the loader so that shimmed transactions are the loader's native
 * transactions, and thus interoperate with transactions opened by other mods.
 *
 * <p>Handles are opaque native transaction objects.
 */
public interface TransactionBackend {

    /**
     * Opens a new root transaction, throwing if a transaction is already open on this thread.
     */
    Object openRoot();

    /**
     * Opens a new transaction nested in the given (current, open) parent.
     */
    Object openNested(Object parent);

    void commit(Object handle);

    /**
     * Closes the transaction, aborting it if it was not committed. Does nothing if it is already closed.
     */
    void close(Object handle);

    int depth(Object handle);

    /**
     * {@return the open transaction at the given depth in the stack containing the given transaction}
     */
    Object getOpenTransaction(Object handle, int depth);

    /**
     * Registers a callback that is invoked when the given transaction closes, with {@code true} if it was aborted.
     */
    void addCloseCallback(Object handle, Consumer<Boolean> onClose);

    /**
     * Registers a callback that is invoked after the outermost (root) transaction containing the given transaction closes, with {@code true}
     * if it was committed.
     */
    void addRootCloseCallback(Object handle, Consumer<Boolean> afterRootClose);

    /**
     * {@return the innermost open transaction on this thread, or {@code null} if there is none}
     *
     * @throws IllegalStateException if a transaction is currently closing
     */
    @Nullable
    Object currentOpen();

    Transaction.Lifecycle lifecycle();
}
