package mekanism.generators.client;

import mekanism.client.platform.ClientServices;
import mekanism.generators.common.GeneratorsLang;
import mekanism.generators.common.MekanismGenerators;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(value = MekanismGenerators.MODID, dist = Dist.CLIENT)
public class GeneratorsClient {

    public GeneratorsClient(ModContainer container) {
        ClientServices.PLATFORM.registerClientExtensions(container, GeneratorsLang.MEKANISM_GENERATORS::translate);
    }
}