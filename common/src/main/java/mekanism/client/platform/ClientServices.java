package mekanism.client.platform;

import mekanism.common.platform.Services;

/**
 * Entry point for loader agnostic client code to access loader specific client functionality.
 */
public final class ClientServices {

    private ClientServices() {
    }

    public static final IClientPlatformHelper PLATFORM = Services.load(IClientPlatformHelper.class);
}
