package mekanism.fabric.integration.jei;

import mekanism.client.recipe_viewer.jei.MekanismJeiScreenHooks;
import mekanism.common.Mekanism;
import mezz.jei.api.IModPlugin;
import net.minecraft.resources.Identifier;

/**
 * Fabric only JEI plugin (registered through the {@code jei_mod_plugin} entrypoint, so it is only loaded when JEI is present) that sets up what NeoForge
 * does through {@code @Mod(depends = "jei")} classes: {@link MekanismJeiScreenHooks}.
 */
public class MekanismFabricJeiPlugin implements IModPlugin {

    private static boolean screenHooksRegistered;

    public MekanismFabricJeiPlugin() {
        if (!screenHooksRegistered) {
            screenHooksRegistered = true;
            new MekanismJeiScreenHooks();
        }
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Mekanism.MODID, "jei_plugin_fabric");
    }
}
