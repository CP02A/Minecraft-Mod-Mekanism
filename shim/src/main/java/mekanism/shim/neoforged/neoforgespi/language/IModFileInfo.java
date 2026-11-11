/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package mekanism.shim.neoforged.neoforgespi.language;

import java.util.List;

/**
 * Mekanism shim: The subset of FML's mod file info used by the shared code.
 */
public interface IModFileInfo {

    List<IModInfo> getMods();
}
