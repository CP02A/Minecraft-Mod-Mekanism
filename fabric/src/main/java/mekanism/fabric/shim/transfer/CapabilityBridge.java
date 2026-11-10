package mekanism.fabric.shim.transfer;

import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import org.jspecify.annotations.Nullable;

/**
 * Bridges NeoForge's item and fluid block capabilities with Fabric's item and fluid storage lookups, in both directions:
 * <ul>
 *     <li>Blocks of shim mods are exposed to Fabric mods through Fabric's lookups.</li>
 *     <li>Blocks that don't provide NeoForge's capability (such as blocks of other Fabric mods) are exposed to shim mods through their Fabric storages.</li>
 * </ul>
 */
public final class CapabilityBridge {

    private CapabilityBridge() {
    }

    /**
     * Called once capabilities have been registered.
     */
    public static void register() {
        Set<String> shimNamespaces = ModList.get().getModContainers().stream().map(ModContainer::getNamespace).collect(Collectors.toSet());
        bridge(Capabilities.Item.BLOCK, ItemStorage.SIDED, ResourceConversion.ITEM, shimNamespaces);
        bridge(Capabilities.Fluid.BLOCK, FluidStorage.SIDED, ResourceConversion.FLUID, shimNamespaces);
    }

    private static <R extends Resource, V extends TransferVariant<?>> void bridge(BlockCapability<ResourceHandler<R>, @Nullable Direction> capability,
          BlockApiLookup<Storage<V>, @Nullable Direction> lookup, ResourceConversion<R, V> conversion, Set<String> shimNamespaces) {
        //Shim mod blocks -> Fabric
        Block[] blocks = capability.getProvidingBlocks().stream()
              .filter(block -> shimNamespaces.contains(BuiltInRegistries.BLOCK.getKey(block).getNamespace()))
              .toArray(Block[]::new);
        if (blocks.length > 0) {
            lookup.registerForBlocks((level, pos, state, blockEntity, side) -> {
                ResourceHandler<R> handler = capability.getCapability(level, pos, state, blockEntity, side);
                if (handler == null) {
                    return null;
                } else if (handler instanceof FabricStorageResourceHandler<R, ?> fabric) {
                    //noinspection unchecked
                    return (Storage<V>) fabric.getStorage();
                }
                return new ResourceHandlerStorage<>(handler, conversion);
            }, blocks);
        }
        //Fabric -> shim mods, for blocks without providers of the NeoForge capability
        capability.setLoaderFallback((level, pos, state, blockEntity, side) -> {
            Storage<V> storage = lookup.find(level, pos, state, blockEntity, side);
            return storage == null ? null : new FabricStorageResourceHandler<>(storage, conversion);
        });
    }
}
