package mekanism.fabric;

import mekanism.common.platform.MekanismCommon;
import net.fabricmc.api.ModInitializer;

public class MekanismFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        MekanismCommon.init();
    }
}
