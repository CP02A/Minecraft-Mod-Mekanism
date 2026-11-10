/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.language;

import org.apache.maven.artifact.versioning.ArtifactVersion;

/**
 * Mekanism shim: The subset of FML's mod info used by the shared code.
 */
public interface IModInfo {

    String getModId();

    String getDisplayName();

    String getDescription();

    ArtifactVersion getVersion();

    default String getNamespace() {
        return getModId();
    }
}
