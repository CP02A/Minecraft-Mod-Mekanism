/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.network.handling;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

/**
 * Mekanism shim: The subset of NeoForge's payload context used by the shared code, implemented by the loader.
 */
public interface IPayloadContext {

    /**
     * Sends a payload back to the sender of the payload being handled.
     */
    void reply(CustomPacketPayload payload);

    /**
     * Disconnects the connection the payload was received on.
     */
    void disconnect(Component reason);

    /**
     * {@return the player associated with the connection; on the client this is the local player}
     *
     * @throws UnsupportedOperationException If called during the configuration phase.
     */
    Player player();

    /**
     * {@return the direction the payload was sent in}
     */
    PacketFlow flow();

    /**
     * Runs the given task on the main thread of the receiving side.
     */
    CompletableFuture<Void> enqueueWork(Runnable task);

    /**
     * Runs the given task on the main thread of the receiving side.
     */
    <T> CompletableFuture<T> enqueueWork(Supplier<T> task);
}
