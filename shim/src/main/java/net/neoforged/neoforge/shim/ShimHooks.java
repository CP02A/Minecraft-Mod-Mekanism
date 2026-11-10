/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker;
import net.neoforged.neoforge.capabilities.CapabilityListenerHolder;
import net.neoforged.neoforge.capabilities.ICapabilityInvalidationListener;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla-only implementations of the methods NeoForge patches into vanilla classes that the shimmed classes rely on.
 */
public final class ShimHooks {

    private ShimHooks() {
    }

    private static final Map<Item, ItemResource> DEFAULT_ITEM_RESOURCES = new ConcurrentHashMap<>();
    private static final Map<Fluid, FluidResource> DEFAULT_FLUID_RESOURCES = new ConcurrentHashMap<>();
    private static final Map<Fluid, FluidType> FLUID_TYPES = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, CapabilityListenerHolder> CAPABILITY_LISTENERS = Collections.synchronizedMap(new WeakHashMap<>());

    private static final Map<TicketStorage, ForcedChunks> FORCED_CHUNKS = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * The owner tracked forced chunks of a ticket storage.
     */
    public record ForcedChunks(TicketTracker<BlockPos> blocks, TicketTracker<UUID> entities) {
    }

    /**
     * {@return the owner tracked forced chunks of the given ticket storage, which NeoForge stores in the ticket storage itself}
     */
    public static ForcedChunks getForcedChunks(TicketStorage storage) {
        return FORCED_CHUNKS.computeIfAbsent(storage, s -> new ForcedChunks(
              new TicketTracker<>(s, NeoForgeMod.BLOCK_TICKET, NeoForgeMod.BLOCK_WITH_NATURAL_SPAWNING_TICKET),
              new TicketTracker<>(s, NeoForgeMod.ENTITY_TICKET, NeoForgeMod.ENTITY_WITH_NATURAL_SPAWNING_TICKET)
        ));
    }

    /**
     * Set by the loader once capabilities have been registered.
     */
    public static boolean capabilitiesInitialized;

    /**
     * {@return the capability listeners of the given level, which NeoForge stores on the level itself}
     */
    public static CapabilityListenerHolder getCapabilityListeners(ServerLevel level) {
        return CAPABILITY_LISTENERS.computeIfAbsent(level, l -> new CapabilityListenerHolder());
    }

    /**
     * Replacement for NeoForge's {@code ServerLevel#registerCapabilityListener}.
     */
    public static void registerCapabilityListener(ServerLevel level, BlockPos pos, ICapabilityInvalidationListener listener) {
        getCapabilityListeners(level).addListener(pos, listener);
    }

    /**
     * Replacement for NeoForge's {@code Level#invalidateCapabilities(BlockPos)}.
     */
    public static void invalidateCapabilities(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            CapabilityListenerHolder holder = CAPABILITY_LISTENERS.get(serverLevel);
            if (holder != null) {
                holder.invalidatePos(pos);
            }
        }
    }

    /**
     * Replacement for NeoForge's {@code Level#invalidateCapabilities(ChunkPos)}.
     */
    public static void invalidateCapabilities(Level level, ChunkPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            CapabilityListenerHolder holder = CAPABILITY_LISTENERS.get(serverLevel);
            if (holder != null) {
                holder.invalidateChunk(pos);
            }
        }
    }

    /**
     * Replacement for NeoForge's per item cached default resource.
     */
    public static ItemResource computeDefaultResource(Item item, Function<Item, ItemResource> creator) {
        return DEFAULT_ITEM_RESOURCES.computeIfAbsent(item, creator);
    }

    /**
     * Replacement for NeoForge's per fluid cached default resource.
     */
    public static FluidResource computeDefaultResource(Fluid fluid, Function<Fluid, FluidResource> creator) {
        return DEFAULT_FLUID_RESOURCES.computeIfAbsent(fluid, creator);
    }

    /**
     * Replacement for NeoForge's {@code Fluid#getFluidType()}.
     */
    public static FluidType getFluidType(Fluid fluid) {
        if (fluid instanceof BaseFlowingFluid flowingFluid) {
            return flowingFluid.getFluidType();
        } else if (fluid == Fluids.EMPTY) {
            return NeoForgeMod.EMPTY_TYPE.value();
        } else if (fluid.isSame(Fluids.WATER)) {
            return NeoForgeMod.WATER_TYPE.value();
        } else if (fluid.isSame(Fluids.LAVA)) {
            return NeoForgeMod.LAVA_TYPE.value();
        }
        //Fluids from mods that don't use NeoForge style fluid types
        return FLUID_TYPES.computeIfAbsent(fluid, ShimServices.BACKEND::getFallbackFluidType);
    }

    public static boolean isComponentsPatchEmpty(ItemStack stack) {
        return stack.getComponentsPatch().isEmpty();
    }

    public static boolean isPatchEmpty(PatchedDataComponentMap components) {
        return components.asPatch().isEmpty();
    }

    public static boolean patchEquals(PatchedDataComponentMap components, DataComponentPatch patch) {
        return components.asPatch().equals(patch);
    }

    /**
     * {@return true if the stack and the template are both empty, or represent the same item with the same components}
     */
    public static boolean isSameItemSameComponents(ItemStack stack, @Nullable ItemStackTemplate template) {
        if (stack.isEmpty() || template == null) {
            return stack.isEmpty() == (template == null);
        }
        return stack.is(template.item()) && stack.getComponentsPatch().equals(template.components());
    }

    public static boolean isImmediatelyResolvable(HolderSet<?> set) {
        return set.isImmediatelyResolvable();
    }

    /**
     * Precomputed view of a {@link DataComponentPatch} to answer the queries NeoForge adds to it.
     */
    public record PatchView(DataComponentMap added, java.util.Set<DataComponentType<?>> removed) {

        public static PatchView of(DataComponentPatch patch) {
            DataComponentPatch.SplitResult split = patch.split();
            return new PatchView(split.added(), split.removed());
        }

        public boolean isPatched(DataComponentType<?> type) {
            return added.has(type) || removed.contains(type);
        }

        public java.util.Set<DataComponentType<?>> keySet() {
            java.util.Set<DataComponentType<?>> keys = new java.util.HashSet<>(added.keySet());
            keys.addAll(removed);
            return keys;
        }

        /**
         * {@return the patched value of the component, or {@code null} if the component is removed by the patch}
         */
        @Nullable
        public Object getPatch(DataComponentType<?> type) {
            return added.get(type);
        }
    }
}
