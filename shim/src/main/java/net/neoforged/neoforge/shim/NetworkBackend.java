/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;

/**
 * Backend for the shimmed NeoForge networking API, implemented using the loader's networking API.
 */
public interface NetworkBackend {

    enum Direction {
        TO_CLIENT,
        TO_SERVER
    }

    <T extends CustomPacketPayload> void registerPlay(Direction direction, CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
          IPayloadHandler<T> handler, HandlerThread thread);

    <T extends CustomPacketPayload> void registerConfiguration(Direction direction, CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
          IPayloadHandler<T> handler, HandlerThread thread);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendToAllPlayers(CustomPacketPayload payload);

    void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload);

    void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload payload);

    void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, boolean includeSelf);

    void sendToServer(CustomPacketPayload payload);
}
