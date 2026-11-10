/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.transfer.transaction;

import net.neoforged.neoforge.shim.ShimServices;
import net.neoforged.neoforge.shim.TransactionBackend;
import org.jspecify.annotations.Nullable;

/**
 * A global operation that guarantees either the whole operation succeeds, or it is completely aborted and rolls back.
 *
 * <p>Mekanism shim: Same public API as NeoForge's transactions, but each transaction is a thin wrapper around the current loader's native
 * transaction (see {@link TransactionBackend}), so that nesting works with transactions opened by other mods.
 */
public final class Transaction implements AutoCloseable, TransactionContext {

    private static final TransactionBackend BACKEND = ShimServices.TRANSACTIONS;
    /**
     * Native transaction objects are reused per depth, so cache the wrapper for each depth to avoid allocating a new wrapper every time a
     * transaction is opened or passed to us from outside.
     */
    private static final ThreadLocal<Transaction[]> WRAPPERS = ThreadLocal.withInitial(() -> new Transaction[8]);

    /**
     * Opens a new root transaction.
     *
     * @throws IllegalStateException If a transaction is already open or closing on the current thread.
     */
    public static Transaction openRoot() {
        return wrap(BACKEND.openRoot());
    }

    /**
     * Opens a new transaction. If the parent is {@code null}, this opens a root transaction, otherwise a transaction nested in the parent.
     *
     * @throws IllegalStateException If the parent is not the current transaction, or if the parent is {@code null} and a transaction is
     *                               already active on this thread.
     */
    public static Transaction open(@Nullable TransactionContext parent) {
        return wrap(parent == null ? BACKEND.openRoot() : BACKEND.openNested(((Transaction) parent).handle));
    }

    /**
     * {@return the current lifecycle of the transaction stack on this thread}
     */
    public static Lifecycle getLifecycle() {
        return BACKEND.lifecycle();
    }

    /**
     * {@return the currently open transaction, or {@code null} if there is none}
     *
     * @deprecated Only use this if you absolutely need to, as passing the transaction context explicitly is much safer.
     */
    @Nullable
    @Deprecated
    public static TransactionContext getCurrentOpenedTransaction() {
        Object current = BACKEND.currentOpen();
        return current == null ? null : wrap(current);
    }

    /**
     * Wraps a native transaction of the current loader.
     *
     * @apiNote Mekanism shim only, used by loader specific code to convert native transactions passed in by other mods.
     */
    public static Transaction wrap(Object handle) {
        int depth = BACKEND.depth(handle);
        Transaction[] wrappers = WRAPPERS.get();
        if (depth >= wrappers.length) {
            Transaction[] grown = new Transaction[Math.max(depth + 1, wrappers.length * 2)];
            System.arraycopy(wrappers, 0, grown, 0, wrappers.length);
            WRAPPERS.set(wrappers = grown);
        }
        Transaction wrapper = wrappers[depth];
        if (wrapper == null || wrapper.handle != handle) {
            wrappers[depth] = wrapper = new Transaction(handle);
        }
        return wrapper;
    }

    final Object handle;

    private Transaction(Object handle) {
        this.handle = handle;
    }

    /**
     * {@return the native transaction of the current loader that backs this transaction}
     *
     * @apiNote Mekanism shim only.
     */
    public Object nativeHandle() {
        return handle;
    }

    /**
     * Closes the current transaction, committing all the changes that happened during this transaction and the transactions opened inside
     * of it.
     */
    public void commit() {
        BACKEND.commit(handle);
    }

    /**
     * Closes the current transaction, cancelling all the changes that happened during this transaction and the transactions opened inside
     * of it, if it has not already been committed.
     */
    @Override
    public void close() {
        BACKEND.close(handle);
    }

    @Override
    public int depth() {
        return BACKEND.depth(handle);
    }

    @Override
    public String toString() {
        return "Transaction[" + handle + "]";
    }

    /**
     * The lifecycle of the transaction stack of a thread.
     */
    public enum Lifecycle {
        /**
         * No transaction is currently open or closing.
         */
        NONE,
        /**
         * A transaction is currently open.
         */
        OPEN,
        /**
         * The current transaction is invoking its close callbacks.
         */
        CLOSING,
        /**
         * The root transaction is invoking its root commit callbacks.
         */
        ROOT_CLOSING
    }
}
