/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import org.jspecify.annotations.Nullable;

/**
 * Backend for NeoForge style data attachments, implemented using the loader's attachment API. Holders are entities, block entities,
 * chunks and levels.
 */
public interface AttachmentBackend {

    boolean hasAttachments(IAttachmentHolder holder);

    boolean hasData(IAttachmentHolder holder, AttachmentType<?> type);

    /**
     * {@return the data of the given type, creating it with the type's default value if it is not present}
     */
    <T> T getData(IAttachmentHolder holder, AttachmentType<T> type);

    <T> @Nullable T getExistingData(IAttachmentHolder holder, AttachmentType<T> type);

    <T> @Nullable T setData(IAttachmentHolder holder, AttachmentType<T> type, T data);

    <T> @Nullable T removeData(IAttachmentHolder holder, AttachmentType<T> type);

    void syncData(IAttachmentHolder holder, AttachmentType<?> type);
}
