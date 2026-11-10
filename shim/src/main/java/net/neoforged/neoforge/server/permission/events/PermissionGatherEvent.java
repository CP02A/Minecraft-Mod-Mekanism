/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.server.permission.events;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;

/**
 * Fired to gather information for the permission API.
 *
 * <p>Mekanism shim: Custom permission handlers are not supported.
 */
public abstract class PermissionGatherEvent extends Event {

    /**
     * Fired to gather the permission nodes that should be registered.
     */
    public static class Nodes extends PermissionGatherEvent {

        private final Set<PermissionNode<?>> nodes = new HashSet<>();

        public Nodes() {
        }

        public Collection<PermissionNode<?>> getNodes() {
            return Collections.unmodifiableCollection(this.nodes);
        }

        public void addNodes(PermissionNode<?>... nodes) {
            for (PermissionNode<?> node : nodes) {
                if (!this.nodes.add(node)) {
                    throw new IllegalArgumentException("Tried to register duplicate PermissionNode '" + node.getNodeName() + "'");
                }
            }
        }

        public void addNodes(Iterable<PermissionNode<?>> nodes) {
            for (PermissionNode<?> node : nodes) {
                if (!this.nodes.add(node)) {
                    throw new IllegalArgumentException("Tried to register duplicate PermissionNode '" + node.getNodeName() + "'");
                }
            }
        }
    }
}
