package mekanism.common.integration.lookingat.theoneprobe;

import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.TheOneProbePlugin;

/**
 * Fabric registration of {@link TOPProvider} through The One Probe's {@code theoneprobe} entrypoint (NeoForge uses IMC, see MekanismTOPIntegration).
 */
public class MekanismTOPPlugin implements TheOneProbePlugin {

    @Override
    public void register(ITheOneProbe probe) {
        new TOPProvider().apply(probe);
    }
}
