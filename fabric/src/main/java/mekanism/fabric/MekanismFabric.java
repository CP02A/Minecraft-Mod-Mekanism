package mekanism.fabric;

import mekanism.fabric.shim.ShimModLoader;
import net.fabricmc.api.ModInitializer;

public class MekanismFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        //Mekanism is constructed by the shim's mod loader via its neoforge-shim entrypoint (MekanismShimMod)
        ShimModLoader.loadAll();
    }
}
