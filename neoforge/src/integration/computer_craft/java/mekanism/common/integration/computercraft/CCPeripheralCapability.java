package mekanism.common.integration.computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

/// Loader specific access to the block capability CC: Tweaked looks up peripherals with. On NeoForge this is CC's own [PeripheralCapability].
public class CCPeripheralCapability {

    private CCPeripheralCapability() {
    }

    public static BlockCapability<IPeripheral, @Nullable Direction> get() {
        return PeripheralCapability.get();
    }
}
