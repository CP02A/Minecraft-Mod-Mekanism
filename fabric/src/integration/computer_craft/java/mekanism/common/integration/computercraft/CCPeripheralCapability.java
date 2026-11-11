package mekanism.common.integration.computercraft;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.BlockCapability;
import org.jspecify.annotations.Nullable;

/// Loader specific access to the block capability CC: Tweaked looks up peripherals with. CC: Tweaked's Fabric version looks them up through
/// [PeripheralLookup] (a Fabric API block lookup), so Mekanism provides peripherals through a capability of the shim with the same name, which
/// [#registerLookup()] exposes through CC's lookup. This keeps the NeoForge behaviour of Mekanism's capability resolvers (caching, invalidation) and
/// the bounding block proxies.
public class CCPeripheralCapability {

    private static final BlockCapability<IPeripheral, @Nullable Direction> PERIPHERAL = BlockCapability.createSided(PeripheralLookup.ID, IPeripheral.class);

    private CCPeripheralCapability() {
    }

    public static BlockCapability<IPeripheral, @Nullable Direction> get() {
        return PERIPHERAL;
    }

    /// Registers the blocks that provide the peripheral capability with CC's [PeripheralLookup]. Called once all capabilities have been registered.
    public static void registerLookup() {
        Block[] blocks = PERIPHERAL.getProvidingBlocks().toArray(Block[]::new);
        if (blocks.length > 0) {
            PeripheralLookup.get().registerForBlocks((level, pos, state, blockEntity, side) -> PERIPHERAL.getCapability(level, pos, state, blockEntity, side), blocks);
        }
    }
}
