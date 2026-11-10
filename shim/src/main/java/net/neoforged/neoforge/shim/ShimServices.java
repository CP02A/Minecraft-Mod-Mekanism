/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim;

import java.util.ServiceLoader;

/**
 * Loads the loader provided implementations of the shim's backend services.
 */
public final class ShimServices {

    private ShimServices() {
    }

    public static final TransactionBackend TRANSACTIONS = load(TransactionBackend.class);
    public static final ShimBackend BACKEND = load(ShimBackend.class);
    public static final NetworkBackend NETWORK = load(NetworkBackend.class);
    public static final AttachmentBackend ATTACHMENTS = load(AttachmentBackend.class);
    /**
     * Only available on the client.
     */
    public static final ClientBackend CLIENT = BACKEND.getDist() == net.neoforged.api.distmarker.Dist.CLIENT ? loadOptional(ClientBackend.class) : null;

    @SuppressWarnings("DataFlowIssue")
    private static <T> T loadOptional(Class<T> serviceClass) {
        return ServiceLoader.load(serviceClass, ShimServices.class.getClassLoader()).findFirst().orElse(null);
    }

    public static <T> T load(Class<T> serviceClass) {
        return ServiceLoader.load(serviceClass, ShimServices.class.getClassLoader())
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Missing NeoForge shim backend for " + serviceClass.getName()));
    }
}
