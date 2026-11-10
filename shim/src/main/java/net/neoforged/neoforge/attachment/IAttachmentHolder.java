/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.attachment;

import net.neoforged.neoforge.shim.ShimServices;
import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * An object that can hold data attachments.
 */
public interface IAttachmentHolder {
    /**
     * Returns {@code true} if there is any data attachments, {@code false} otherwise.
     */
    default boolean hasAttachments() {
        return ShimServices.ATTACHMENTS.hasAttachments(this);
    }

    /**
     * Returns {@code true} if there is a data attachment of the give type, {@code false} otherwise.
     */
    default boolean hasData(AttachmentType<?> type) {
        return ShimServices.ATTACHMENTS.hasData(this, type);
    }

    /**
     * Returns {@code true} if there is a data attachment of the give type, {@code false} otherwise.
     */
    default <T> boolean hasData(Supplier<AttachmentType<T>> type) {
        //Note: The unused T generic is necessary so that DeferredHolders can be properly matched as a Supplier
        return hasData(type.get());
    }

    /**
     * {@return the data attachment of the given type}
     *
     * <p>If there is no data attachment of the given type, <b>the default value is stored in this holder and returned.</b>
     */
    default <T> T getData(AttachmentType<T> type) {
        return ShimServices.ATTACHMENTS.getData(this, type);
    }

    /**
     * {@return the data attachment of the given type}
     *
     * <p>If there is no data attachment of the given type, <b>the default value is stored in this holder and returned.</b>
     */
    default <T> T getData(Supplier<AttachmentType<T>> type) {
        return getData(type.get());
    }

    /**
     * {@return an optional possibly containing a data attachment value of the given type}
     *
     * <p>If there is no data attachment of the given type, an empty optional is returned.
     */
    default <T> Optional<T> getExistingData(AttachmentType<T> type) {
        return Optional.ofNullable(getExistingDataOrNull(type));
    }

    /**
     * {@return an optional possibly containing a data attachment value of the given type}
     *
     * <p>If there is no data attachment of the given type, an empty optional is returned.
     */
    default <T> Optional<T> getExistingData(Supplier<AttachmentType<T>> type) {
        return getExistingData(type.get());
    }

    /**
     * @return an existing data attachment value of the given type, or null if there is no data attachment of the given type
     */
    @Nullable
    default <T> T getExistingDataOrNull(AttachmentType<T> type) {
        return ShimServices.ATTACHMENTS.getExistingData(this, type);
    }

    /**
     * @return an existing data attachment value of the given type, or null if there is no data attachment of the given type
     */
    @Nullable
    default <T> T getExistingDataOrNull(Supplier<AttachmentType<T>> type) {
        return getExistingDataOrNull(type.get());
    }

    /**
     * Sets the data attachment of the given type.
     *
     * @return the previous value for that attachment type, if any, or {@code null} if there was none
     */
    default <T> @Nullable T setData(AttachmentType<T> type, T data) {
        return ShimServices.ATTACHMENTS.setData(this, type, data);
    }

    /**
     * Sets the data attachment of the given type.
     *
     * @return the previous value for that attachment type, if any, or {@code null} if there was none
     */
    default <T> @Nullable T setData(Supplier<AttachmentType<T>> type, T data) {
        return setData(type.get(), data);
    }

    /**
     * Removes the data attachment of the given type.
     *
     * @return the previous value for that attachment type, if any, or {@code null} if there was none
     */
    default <T> @Nullable T removeData(AttachmentType<T> type) {
        return ShimServices.ATTACHMENTS.removeData(this, type);
    }

    /**
     * Removes the data attachment of the given type.
     *
     * @return the previous value for that attachment type, if any, or {@code null} if there was none
     */
    default <T> @Nullable T removeData(Supplier<AttachmentType<T>> type) {
        return removeData(type.get());
    }

    /**
     * Syncs a data attachment of the given type with all relevant clients.
     *
     * <p>If there is currently no attachment of the given type,
     * the removal of the attachment is synced to the client.
     *
     * @see AttachmentSyncHandler
     */
    default void syncData(AttachmentType<?> type) {
        ShimServices.ATTACHMENTS.syncData(this, type);
    }

    /**
     * Syncs a data attachment of the given type with all relevant clients.
     *
     * <p>If there is currently no attachment of the given type,
     * the removal of the attachment is synced to the client.
     *
     * @see AttachmentSyncHandler
     */
    default void syncData(Supplier<? extends AttachmentType<?>> type) {
        syncData(type.get());
    }
}
