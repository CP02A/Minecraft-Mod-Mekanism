/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker;
import net.neoforged.neoforge.shim.ShimHooks;

/**
 * Mekanism shim: The owner tracked forced chunks NeoForge patches into {@link TicketStorage}, injected as an interface.
 */
public interface ITicketStorageExtension {

    default TicketTracker<BlockPos> getBlockForcedChunks() {
        return ShimHooks.getForcedChunks((TicketStorage) this).blocks();
    }

    default TicketTracker<UUID> getEntityForcedChunks() {
        return ShimHooks.getForcedChunks((TicketStorage) this).entities();
    }
}
