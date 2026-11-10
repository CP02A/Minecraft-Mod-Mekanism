/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.extensions;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.InFluidPredicate;
import net.neoforged.neoforge.capabilities.EntityCapability;
import org.jspecify.annotations.Nullable;

/**
 * Mekanism shim: Subset of NeoForge's entity extensions, injected into {@link Entity}.
 */
public interface IEntityExtension {

    default <T, C extends @Nullable Object> @Nullable T getCapability(EntityCapability<T, C> capability, C context) {
        return capability.getCapability((Entity) this, context);
    }

    default <T> @Nullable T getCapability(EntityCapability<T, @Nullable Void> capability) {
        return capability.getCapability((Entity) this, null);
    }

    /**
     * Called when the entity is removed from the level.
     */
    default void onRemovedFromLevel() {
    }

    /**
     * Returns whether any fluid the entity is currently in matches the specified condition.
     *
     * @apiNote Mekanism shim: Only vanilla's water and lava are detected.
     */
    default boolean isInFluidType(InFluidPredicate<Entity> predicate) {
        Entity self = (Entity) this;
        double waterHeight = self.getFluidHeight(FluidTags.WATER);
        if (waterHeight > 0 && predicate.test(self, NeoForgeMod.WATER_TYPE.value(), waterHeight)) {
            return true;
        }
        double lavaHeight = self.getFluidHeight(FluidTags.LAVA);
        return lavaHeight > 0 && predicate.test(self, NeoForgeMod.LAVA_TYPE.value(), lavaHeight);
    }

    /**
     * {@return whether the entity can swim in the given fluid type}
     */
    default boolean canSwimInFluidType(FluidType type) {
        return type.canSwim((Entity) this);
    }

    /**
     * {@return the height of the given fluid type the entity is in}
     *
     * @apiNote Mekanism shim: Only vanilla's water and lava are tracked.
     */
    default double getFluidHeight(FluidType type) {
        Entity self = (Entity) this;
        if (type == NeoForgeMod.WATER_TYPE.value()) {
            return self.getFluidHeight(FluidTags.WATER);
        } else if (type == NeoForgeMod.LAVA_TYPE.value()) {
            return self.getFluidHeight(FluidTags.LAVA);
        }
        return 0;
    }
}
