package mekanism.fabric.shim;

import mekanism.shim.neoforged.bus.api.BusBuilder;
import mekanism.shim.neoforged.bus.api.IEventBus;
import mekanism.shim.neoforged.fml.ModContainer;
import mekanism.shim.neoforged.fml.event.IModBusEvent;
import mekanism.shim.neoforged.neoforgespi.language.IModInfo;

/**
 * Container for a mod that uses the shim, with its own mod event bus.
 */
public final class FabricModContainer extends ModContainer {

    private final IEventBus eventBus;

    public FabricModContainer(IModInfo info) {
        super(info);
        this.eventBus = BusBuilder.builder()
              .markerType(IModBusEvent.class)
              .allowPerPhasePost()
              .build();
    }

    @Override
    public IEventBus getEventBus() {
        return eventBus;
    }
}
