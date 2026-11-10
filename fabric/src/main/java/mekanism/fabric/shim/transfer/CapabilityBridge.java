package mekanism.fabric.shim.transfer;

import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import org.jspecify.annotations.Nullable;
import team.reborn.energy.api.EnergyStorage;

/**
 * Bridges NeoForge's item, fluid and energy capabilities with Fabric's item and fluid storage lookups and Team Reborn's energy lookups, for both
 * blocks and items, in both directions:
 * <ul>
 *     <li>Blocks/items of shim mods are exposed to Fabric mods through Fabric's lookups.</li>
 *     <li>Blocks/items that don't provide NeoForge's capability (such as those of other Fabric mods) are exposed to shim mods through their Fabric
 *     storages.</li>
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
        bridgeResource(Capabilities.Item.BLOCK, Capabilities.Item.ITEM, ItemStorage.SIDED, ItemStorage.ITEM, ResourceConversion.ITEM, shimNamespaces);
        bridgeResource(Capabilities.Fluid.BLOCK, Capabilities.Fluid.ITEM, FluidStorage.SIDED, FluidStorage.ITEM, ResourceConversion.FLUID, shimNamespaces);
        Function<EnergyHandler, EnergyStorage> energyToFabric = handler -> handler instanceof StorageEnergyHandler(EnergyStorage storage) ? storage
                                                                                                                                       : new EnergyHandlerStorage(handler);
        Function<EnergyStorage, EnergyHandler> energyToNeo = storage -> storage instanceof EnergyHandlerStorage(EnergyHandler handler) ? handler
                                                                                                                                        : new StorageEnergyHandler(storage);
        bridgeBlock(Capabilities.Energy.BLOCK, EnergyStorage.SIDED, energyToFabric, energyToNeo, shimNamespaces);
        bridgeItem(Capabilities.Energy.ITEM, EnergyStorage.ITEM, energyToFabric, energyToNeo, shimNamespaces);
    }

    private static <R extends Resource, V extends TransferVariant<?>> void bridgeResource(BlockCapability<ResourceHandler<R>, @Nullable Direction> blockCapability,
          ItemCapability<ResourceHandler<R>, ItemAccess> itemCapability, BlockApiLookup<Storage<V>, @Nullable Direction> blockLookup,
          ItemApiLookup<Storage<V>, ContainerItemContext> itemLookup, ResourceConversion<R, V> conversion, Set<String> shimNamespaces) {
        Function<ResourceHandler<R>, Storage<V>> toFabric = handler -> {
            if (handler instanceof FabricStorageResourceHandler<R, ?> fabric) {
                //noinspection unchecked
                return (Storage<V>) fabric.getStorage();
            }
            return new ResourceHandlerStorage<>(handler, conversion);
        };
        Function<Storage<V>, ResourceHandler<R>> toNeo = storage -> {
            if (storage instanceof ResourceHandlerStorage<?, V> neo) {
                //noinspection unchecked
                return (ResourceHandler<R>) neo.getHandler();
            }
            return new FabricStorageResourceHandler<>(storage, conversion);
        };
        bridgeBlock(blockCapability, blockLookup, toFabric, toNeo, shimNamespaces);
        bridgeItem(itemCapability, itemLookup, toFabric, toNeo, shimNamespaces);
    }

    private static <T, A> void bridgeBlock(BlockCapability<T, @Nullable Direction> capability, BlockApiLookup<A, @Nullable Direction> lookup,
          Function<T, A> toFabric, Function<A, T> toNeo, Set<String> shimNamespaces) {
        //Shim mod blocks -> Fabric
        Block[] blocks = capability.getProvidingBlocks().stream()
              .filter(isShimEntry(shimNamespaces, BuiltInRegistries.BLOCK::getKey))
              .toArray(Block[]::new);
        if (blocks.length > 0) {
            lookup.registerForBlocks((level, pos, state, blockEntity, side) -> {
                T handler = capability.getCapability(level, pos, state, blockEntity, side);
                return handler == null ? null : toFabric.apply(handler);
            }, blocks);
        }
        //Fabric -> shim mods, for blocks without providers of the NeoForge capability
        capability.setLoaderFallback((level, pos, state, blockEntity, side) -> {
            A api = lookup.find(level, pos, state, blockEntity, side);
            return api == null ? null : toNeo.apply(api);
        });
    }

    private static <T, A> void bridgeItem(ItemCapability<T, ItemAccess> capability, ItemApiLookup<A, ContainerItemContext> lookup,
          Function<T, A> toFabric, Function<A, T> toNeo, Set<String> shimNamespaces) {
        //Shim mod items -> Fabric
        Item[] items = capability.getProvidingItems().stream()
              .filter(isShimEntry(shimNamespaces, BuiltInRegistries.ITEM::getKey))
              .toArray(Item[]::new);
        if (items.length > 0) {
            lookup.registerForItems((stack, context) -> {
                T handler = capability.getCapability(stack, ContextItemAccess.of(context));
                return handler == null ? null : toFabric.apply(handler);
            }, items);
        }
        //Fabric -> shim mods, for items without providers of the NeoForge capability
        capability.setLoaderFallback((stack, access) -> {
            A api = lookup.find(stack, ItemAccessContext.of(stack, access));
            return api == null ? null : toNeo.apply(api);
        });
    }

    private static <O> Predicate<O> isShimEntry(Set<String> shimNamespaces, Function<O, net.minecraft.resources.Identifier> keyGetter) {
        return entry -> shimNamespaces.contains(keyGetter.apply(entry).getNamespace());
    }
}
