package mekanism.common.platform;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Loader agnostic initialization for Mekanism. Called from the entrypoint of each loader specific project.
 */
public final class MekanismCommon {

    public static final String MODID = "mekanism";
    public static final Logger LOGGER = LogUtils.getLogger();

    private MekanismCommon() {
    }

    public static void init() {
        LOGGER.info("Initializing loader agnostic Mekanism code on {}{}", Services.PLATFORM.getPlatformName(),
              Services.PLATFORM.isDevelopmentEnvironment() ? " (development environment)" : "");
    }
}
