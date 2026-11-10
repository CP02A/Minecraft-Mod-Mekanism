package mekanism.common.platform;

import java.util.ServiceLoader;
import mekanism.common.platform.services.IPlatformHelper;

/**
 * Entry point for loader agnostic code to access loader specific functionality.
 */
public final class Services {

    private Services() {
    }

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    /**
     * Loads the single implementation of the given service that the current loader specific project provides.
     *
     * @param serviceClass Service to load.
     *
     * @return The implementation of the service.
     */
    public static <T> T load(Class<T> serviceClass) {
        return ServiceLoader.load(serviceClass, Services.class.getClassLoader())
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Failed to load service for " + serviceClass.getName()));
    }
}
