/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.network.registration;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.shim.NetworkBackend.Direction;
import net.neoforged.neoforge.shim.ShimServices;

/**
 * Mekanism shim: Registers payloads with the loader's networking API.
 *
 * @apiNote Payload versions are not negotiated; both sides are required to have the same version of the mod.
 */
public class PayloadRegistrar {

    private final String version;
    private HandlerThread thread = HandlerThread.MAIN;
    private boolean optional = false;

    public PayloadRegistrar(String version) {
        this.version = version;
    }

    private PayloadRegistrar(PayloadRegistrar source) {
        this.version = source.version;
        this.thread = source.thread;
        this.optional = source.optional;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> reader,
          IPayloadHandler<T> handler) {
        ShimServices.NETWORK.registerPlay(Direction.TO_CLIENT, type, reader, handler, thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> reader,
          IPayloadHandler<T> handler) {
        ShimServices.NETWORK.registerPlay(Direction.TO_SERVER, type, reader, handler, thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> reader,
          IPayloadHandler<T> handler) {
        return playToClient(type, reader, handler).playToServer(type, reader, handler);
    }

    public <T extends CustomPacketPayload> PayloadRegistrar configurationToClient(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> reader,
          IPayloadHandler<T> handler) {
        ShimServices.NETWORK.registerConfiguration(Direction.TO_CLIENT, type, reader, handler, thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar configurationToServer(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> reader,
          IPayloadHandler<T> handler) {
        ShimServices.NETWORK.registerConfiguration(Direction.TO_SERVER, type, reader, handler, thread);
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar configurationBidirectional(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> reader,
          IPayloadHandler<T> handler) {
        return configurationToClient(type, reader, handler).configurationToServer(type, reader, handler);
    }

    /**
     * {@return a new registrar for the given version}
     */
    public PayloadRegistrar versioned(String version) {
        PayloadRegistrar clone = new PayloadRegistrar(version);
        clone.thread = this.thread;
        clone.optional = this.optional;
        return clone;
    }

    /**
     * {@return a new registrar whose payloads are optional}
     */
    public PayloadRegistrar optional() {
        PayloadRegistrar clone = new PayloadRegistrar(this);
        clone.optional = true;
        return clone;
    }

    /**
     * {@return a new registrar whose handlers execute on the given thread}
     */
    public PayloadRegistrar executesOn(HandlerThread thread) {
        PayloadRegistrar clone = new PayloadRegistrar(this);
        clone.thread = thread;
        return clone;
    }
}
