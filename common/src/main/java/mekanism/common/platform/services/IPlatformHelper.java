package mekanism.common.platform.services;

import java.nio.file.Path;

/**
 * Exposes basic information about the mod loader Mekanism is currently running on.
 *
 * @implNote Each loader specific project provides an implementation of this via a {@link java.util.ServiceLoader} service file.
 */
public interface IPlatformHelper {

    /**
     * {@return the name of the mod loader Mekanism is running on}
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given mod id is loaded.
     *
     * @param modId Mod id to check.
     *
     * @return {@code true} if the mod is loaded.
     */
    boolean isModLoaded(String modId);

    /**
     * {@return {@code true} if Mekanism is running in a development environment, {@code false} if it is running in production}
     */
    boolean isDevelopmentEnvironment();

    /**
     * {@return the directory that configs should be stored in}
     */
    Path getConfigDirectory();
}
