/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.event;

import java.util.Map;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

/**
 * Fired to register cauldron interactions.
 *
 * <p>Mekanism shim: The loader provides the known cauldron dispatchers.
 */
public abstract sealed class RegisterCauldronInteractionEvent extends Event implements IModBusEvent {

    protected final Map<String, CauldronInteraction.Dispatcher> dispatchers;

    protected RegisterCauldronInteractionEvent(Map<String, CauldronInteraction.Dispatcher> dispatchers) {
        this.dispatchers = dispatchers;
    }

    public static final class Dispatcher extends RegisterCauldronInteractionEvent {

        public Dispatcher(Map<String, CauldronInteraction.Dispatcher> dispatchers) {
            super(dispatchers);
        }

        public void register(Identifier id, CauldronInteraction.Dispatcher dispatcher) {
            dispatchers.put(id.toShortString(), dispatcher);
        }
    }

    public static final class Interaction extends RegisterCauldronInteractionEvent {

        public Interaction(Map<String, CauldronInteraction.Dispatcher> dispatchers) {
            super(dispatchers);
        }

        private CauldronInteraction.Dispatcher get(Identifier dispatcherId) {
            CauldronInteraction.Dispatcher dispatcher = dispatchers.get(dispatcherId.toShortString());
            if (dispatcher == null) {
                throw new IllegalArgumentException("No dispatcher registered with id: " + dispatcherId);
            }
            return dispatcher;
        }

        public void register(Identifier dispatcherId, Item item, CauldronInteraction interaction) {
            get(dispatcherId).put(item, interaction);
        }

        public void register(Identifier dispatcherId, TagKey<Item> itemTag, CauldronInteraction interaction) {
            get(dispatcherId).put(itemTag, interaction);
        }

        public void registerToAll(Item item, CauldronInteraction interaction) {
            for (CauldronInteraction.Dispatcher dispatcher : dispatchers.values()) {
                dispatcher.put(item, interaction);
            }
        }

        public void registerToAll(TagKey<Item> itemTag, CauldronInteraction interaction) {
            for (CauldronInteraction.Dispatcher dispatcher : dispatchers.values()) {
                dispatcher.put(itemTag, interaction);
            }
        }
    }
}
