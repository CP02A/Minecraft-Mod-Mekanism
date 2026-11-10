/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.attachment;

import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Exposes the settings of attachment types to the loader specific attachment backends.
 */
@ApiStatus.Internal
public final class AttachmentInternals {

    private AttachmentInternals() {
    }

    public static <T> T createDefault(AttachmentType<T> type, IAttachmentHolder holder) {
        return type.defaultValueSupplier.apply(holder);
    }

    @Nullable
    public static <T> IAttachmentSerializer<T> serializer(AttachmentType<T> type) {
        return type.serializer;
    }

    public static boolean copyOnDeath(AttachmentType<?> type) {
        return type.copyOnDeath;
    }

    public static <T> IAttachmentCopyHandler<T> copyHandler(AttachmentType<T> type) {
        return type.copyHandler;
    }

    @Nullable
    public static <T> AttachmentSyncHandler<T> syncHandler(AttachmentType<T> type) {
        return type.syncHandler;
    }
}
