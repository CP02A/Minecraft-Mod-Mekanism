/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.client.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.shim.ShimServices;

/**
 * Mekanism shim: Sends payloads from the client via the loader's networking API.
 */
public final class ClientPacketDistributor {

    private ClientPacketDistributor() {
    }

    public static void sendToServer(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ShimServices.NETWORK.sendToServer(payload);
        for (CustomPacketPayload other : payloads) {
            ShimServices.NETWORK.sendToServer(other);
        }
    }
}
