/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim.extensions;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Mekanism shim: The methods NeoForge patches directly into {@link EntityType.Builder}, injected as an interface.
 */
public interface IEntityTypeBuilderExtension<T extends Entity> {

    @SuppressWarnings("unchecked")
    private EntityType.Builder<T> self() {
        return (EntityType.Builder<T>) (Object) this;
    }

    /**
     * Vanilla derives whether velocity updates are sent from the update interval, so this is a no-op.
     */
    default EntityType.Builder<T> setShouldReceiveVelocityUpdates(boolean value) {
        return self();
    }

    default EntityType.Builder<T> setTrackingRange(int range) {
        return self().clientTrackingRange(range);
    }

    default EntityType.Builder<T> setUpdateInterval(int interval) {
        return self().updateInterval(interval);
    }

    /**
     * Vanilla only uses its own list of entity types (EntityTypes.OP_ONLY_CUSTOM_DATA), so this is a no-op.
     */
    default EntityType.Builder<T> setOnlyOpCanSetNbt(boolean onlyOpCanSetNbt) {
        return self();
    }
}
