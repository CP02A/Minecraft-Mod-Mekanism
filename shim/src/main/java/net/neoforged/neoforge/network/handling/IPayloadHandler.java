/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.network.handling;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Handles a received payload.
 */
@FunctionalInterface
public interface IPayloadHandler<T extends CustomPacketPayload> {

    void handle(T payload, IPayloadContext context);
}
