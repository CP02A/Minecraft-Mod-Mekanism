package mekanism.client.platform.fabric;

import mekanism.client.platform.IClientPlatformHelper;
import net.minecraft.client.gui.Hud;
import net.neoforged.fml.ModContainer;

public class FabricClientPlatformHelper implements IClientPlatformHelper {

    /**
     * Vanilla's height of the health and armor rows above the hotbar.
     */
    private static final int VANILLA_LEFT_HEIGHT = 49;
    /**
     * Vanilla's height of the food and air rows above the hotbar.
     */
    private static final int VANILLA_RIGHT_HEIGHT = 49;

    @Override
    public int getHudLeftHeight(Hud hud) {
        return VANILLA_LEFT_HEIGHT;
    }

    @Override
    public int getHudRightHeight(Hud hud) {
        return VANILLA_RIGHT_HEIGHT;
    }

    @Override
    public void addHudLeftHeight(Hud hud, int height) {
        //Fabric does not track the stacking of HUD elements
    }

    @Override
    public void registerClientExtensions(ModContainer container) {
        //Config screens are provided through Mod Menu's integration on Fabric
    }
}
