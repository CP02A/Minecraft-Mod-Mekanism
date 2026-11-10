/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import java.util.List;
import java.util.Optional;
import net.neoforged.neoforge.shim.ShimServices;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;

/**
 * Mekanism shim: The subset of FML's mod list used by the shared code, backed by the loader.
 */
public abstract class ModList {

    public static ModList get() {
        return ShimServices.BACKEND.getModList();
    }

    public abstract boolean isLoaded(String modId);

    /**
     * {@return the container of the given mod, only present for mods that use the shim}
     */
    public abstract Optional<? extends ModContainer> getModContainerById(String modId);

    /**
     * {@return the containers of all mods that use the shim}
     */
    public abstract List<? extends ModContainer> getModContainers();

    public abstract List<IModInfo> getMods();

    /**
     * {@return the annotation scan data of the mods that use the shim}
     */
    public abstract List<ModFileScanData> getAllScanData();

    public int size() {
        return getMods().size();
    }
}
