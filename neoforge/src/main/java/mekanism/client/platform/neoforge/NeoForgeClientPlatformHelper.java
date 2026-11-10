package mekanism.client.platform.neoforge;

import mekanism.client.platform.IClientPlatformHelper;
import mekanism.common.MekanismLang;
import net.minecraft.client.gui.Hud;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.modlist.ModDisplayInfo;

public class NeoForgeClientPlatformHelper implements IClientPlatformHelper {

    @Override
    public int getHudLeftHeight(Hud hud) {
        return hud.leftHeight;
    }

    @Override
    public int getHudRightHeight(Hud hud) {
        return hud.rightHeight;
    }

    @Override
    public void addHudLeftHeight(Hud hud, int height) {
        hud.leftHeight += height;
    }

    @Override
    public void registerClientExtensions(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        container.<ModDisplayInfo>registerExtensionPoint(ModDisplayInfo.class, () -> new MekanismModDisplayInfo(container, MekanismLang.MEKANISM));
    }
}
