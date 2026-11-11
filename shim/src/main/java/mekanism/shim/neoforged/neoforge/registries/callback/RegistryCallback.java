/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforge.registries.callback;

import mekanism.shim.neoforged.neoforge.registries.IRegistryExtension;

/**
 * Marker interface for registry callbacks.
 * Register to {@link IRegistryExtension#addCallback(RegistryCallback)}.
 */
public sealed interface RegistryCallback<T> permits AddCallback, BakeCallback, ClearCallback {}
