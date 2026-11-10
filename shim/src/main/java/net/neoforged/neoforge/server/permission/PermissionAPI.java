/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.server.permission;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

/**
 * Mekanism shim: Permission queries resolve using each node's default resolver (equivalent to NeoForge's default permission handler).
 */
public final class PermissionAPI {

    private static Collection<PermissionNode<?>> registeredNodes = List.of();

    private PermissionAPI() {
    }

    public static Collection<PermissionNode<?>> getRegisteredNodes() {
        return Collections.unmodifiableCollection(registeredNodes);
    }

    public static <T> T getPermission(ServerPlayer player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        return node.getDefaultResolver().resolve(player, player.getUUID(), context);
    }

    public static <T> T getOfflinePermission(UUID player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        return node.getDefaultResolver().resolve(null, player, context);
    }

    /**
     * @apiNote Mekanism shim: Called by the loader when a server starts.
     */
    public static void initializePermissionAPI() {
        PermissionGatherEvent.Nodes nodesEvent = new PermissionGatherEvent.Nodes();
        NeoForge.EVENT_BUS.post(nodesEvent);
        registeredNodes = List.copyOf(nodesEvent.getNodes());
    }
}
