/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.neoforge.shim.ShimServices;

/**
 * Mekanism shim: The subset of FML's paths used by the shared code.
 */
public enum FMLPaths {
    GAMEDIR,
    MODSDIR,
    CONFIGDIR;

    public Path get() {
        return switch (this) {
            case GAMEDIR -> ShimServices.BACKEND.getGameDirectory();
            case MODSDIR -> ShimServices.BACKEND.getGameDirectory().resolve("mods");
            case CONFIGDIR -> ShimServices.BACKEND.getConfigDirectory();
        };
    }

    public static Path getOrCreateGameRelativePath(Path path) {
        Path gameFolderPath = GAMEDIR.get().resolve(path);
        if (!Files.isDirectory(gameFolderPath)) {
            try {
                Files.createDirectories(gameFolderPath);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return gameFolderPath;
    }
}
