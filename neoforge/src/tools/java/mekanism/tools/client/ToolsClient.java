package mekanism.tools.client;

import mekanism.client.platform.ClientServices;
import mekanism.tools.common.MekanismTools;
import mekanism.tools.common.ToolsLang;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = MekanismTools.MODID, dist = Dist.CLIENT)
public class ToolsClient {

    public ToolsClient(ModContainer container) {
        ClientServices.PLATFORM.registerClientExtensions(container, ToolsLang.MEKANISM_TOOLS::translate);
    }
}