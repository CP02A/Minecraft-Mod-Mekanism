package mekanism.client.platform;

import net.minecraft.client.gui.Hud;
import net.neoforged.fml.ModContainer;

/**
 * Exposes client side functionality that differs between mod loaders.
 *
 * @implNote Each loader specific project provides an implementation of this via a {@link java.util.ServiceLoader} service file.
 */
public interface IClientPlatformHelper {

    /**
     * {@return the height of the HUD elements stacked on the left side above the hotbar}
     */
    int getHudLeftHeight(Hud hud);

    /**
     * {@return the height of the HUD elements stacked on the right side above the hotbar}
     */
    int getHudRightHeight(Hud hud);

    /**
     * Reserves extra height on the left side above the hotbar, so that other HUD elements render above it.
     */
    void addHudLeftHeight(Hud hud, int height);

    /**
     * Registers loader specific extension points of the mod container, such as the config screen and mod list information.
     */
    void registerClientExtensions(ModContainer container);
}
