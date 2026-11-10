/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.shim.ShimServices;

/**
 * Mekanism shim: Sends payloads from the server via the loader's networking API.
 */
public final class PacketDistributor {

    private PacketDistributor() {
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ShimServices.NETWORK.sendToPlayer(player, payload);
        for (CustomPacketPayload other : payloads) {
            ShimServices.NETWORK.sendToPlayer(player, other);
        }
    }

    public static void sendToPlayersInDimension(ServerLevel level, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ShimServices.NETWORK.sendToPlayersInDimension(level, payload);
        for (CustomPacketPayload other : payloads) {
            ShimServices.NETWORK.sendToPlayersInDimension(level, other);
        }
    }

    public static void sendToPlayersTrackingChunk(ServerLevel level, ChunkPos chunkPos, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ShimServices.NETWORK.sendToPlayersTrackingChunk(level, chunkPos, payload);
        for (CustomPacketPayload other : payloads) {
            ShimServices.NETWORK.sendToPlayersTrackingChunk(level, chunkPos, other);
        }
    }

    public static void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ShimServices.NETWORK.sendToPlayersTrackingEntity(entity, payload, false);
        for (CustomPacketPayload other : payloads) {
            ShimServices.NETWORK.sendToPlayersTrackingEntity(entity, other, false);
        }
    }

    public static void sendToPlayersTrackingEntityAndSelf(Entity entity, CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ShimServices.NETWORK.sendToPlayersTrackingEntity(entity, payload, true);
        for (CustomPacketPayload other : payloads) {
            ShimServices.NETWORK.sendToPlayersTrackingEntity(entity, other, true);
        }
    }

    public static void sendToAllPlayers(CustomPacketPayload payload, CustomPacketPayload... payloads) {
        ShimServices.NETWORK.sendToAllPlayers(payload);
        for (CustomPacketPayload other : payloads) {
            ShimServices.NETWORK.sendToAllPlayers(other);
        }
    }
}
