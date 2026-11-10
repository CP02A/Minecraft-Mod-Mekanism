/*
 * Copyright (c) Mekanism contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.shim;

import com.mojang.serialization.Codec;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Registry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.callback.RegistryCallback;
import org.jspecify.annotations.Nullable;

/**
 * Miscellaneous loader specific functionality the shim needs, implemented by the loader.
 */
public interface ShimBackend {

    /**
     * Creates and registers a new root registry.
     *
     * @param sync Whether the registry's contents should be synced to clients.
     */
    <T> Registry<T> createRegistry(ResourceKey<Registry<T>> key, boolean sync);

    /**
     * Records the settings for a registry created via {@link net.neoforged.neoforge.registries.RegistryBuilder}, to be applied when it is
     * registered to the root registry.
     */
    <T> void configureRegistry(Registry<T> registry, boolean sync, List<RegistryCallback<T>> callbacks);

    /**
     * Registers a registry created via {@link net.neoforged.neoforge.registries.RegistryBuilder} to the root registry.
     */
    void registerRootRegistry(Registry<?> registry);

    /**
     * Adds an alias to the registry so that references to {@code from} resolve to {@code to}.
     */
    <T> void addAlias(Registry<T> registry, Identifier from, Identifier to);

    /**
     * Wraps a NeoForge style custom ingredient into a vanilla ingredient using the loader's custom ingredient system.
     */
    Ingredient toVanilla(ICustomIngredient ingredient);

    /**
     * {@return the NeoForge style custom ingredient backing the given ingredient, or {@code null} if it is not one}
     */
    @Nullable
    ICustomIngredient getCustomIngredient(Ingredient ingredient);

    /**
     * {@return {@code true} if the given ingredient only depends on the item, and not on any components of the stack}
     */
    boolean isSimple(Ingredient ingredient);

    /**
     * {@return a fluid type for a fluid that does not have one, generally derived from the loader's own fluid attributes}
     */
    FluidType getFallbackFluidType(Fluid fluid);

    /**
     * {@return the physical distribution the game is running on}
     */
    Dist getDist();

    /**
     * {@return {@code true} if the game is running in production}
     */
    boolean isProduction();

    /**
     * {@return {@code true} if the current thread is the logical server thread}
     */
    boolean isOnServerThread();

    Path getGameDirectory();

    Path getConfigDirectory();

    /**
     * {@return the mod list, containing the mods that use the shim}
     */
    ModList getModList();

    /**
     * Registers a NeoForge style config for the given mod with the loader's config system.
     */
    void registerConfig(ModContainer container, ModConfig.Type type, IConfigSpec spec, @Nullable String fileName);

    /**
     * {@return the last known username of the player with the given UUID, or {@code null} if it is not known}
     */
    @Nullable
    String getLastKnownUsername(UUID uuid);

    /**
     * Registers a datapack (dynamic) registry with the loader.
     *
     * @param networkCodec Codec to sync the registry to clients with, or {@code null} to not sync it.
     * @param reloadable   {@code true} for registries that are reloaded with datapacks (like loot tables), {@code false} for world registries.
     */
    <T> void addDatapackRegistry(ResourceKey<Registry<T>> key, Codec<T> codec, @Nullable Codec<T> networkCodec, boolean reloadable);
}
