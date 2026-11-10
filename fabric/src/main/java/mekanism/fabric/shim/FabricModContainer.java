package mekanism.fabric.shim;

import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforgespi.language.IModInfo;

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
