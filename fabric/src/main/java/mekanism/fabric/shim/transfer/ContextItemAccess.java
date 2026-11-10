package mekanism.fabric.shim.transfer;

import com.google.common.primitives.Ints;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * Exposes a Fabric {@link ContainerItemContext} as a NeoForge {@link ItemAccess}.
 */
public record ContextItemAccess(ContainerItemContext context) implements ItemAccess {

    public static ItemAccess of(ContainerItemContext context) {
        return context instanceof ItemAccessContext(ItemAccess access) ? access : new ContextItemAccess(context);
    }

    @Override
    public ItemResource getResource() {
        return ResourceConversion.ITEM.toResource(context.getItemVariant());
    }

    @Override
    public int getAmount() {
        return Ints.saturatedCast(context.getAmount());
    }

    @Override
    public int insert(ItemResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        return Ints.saturatedCast(context.insert(ResourceConversion.ITEM.toVariant(resource), amount, FabricStorageResourceHandler.toFabric(transaction)));
    }

    @Override
    public int extract(ItemResource resource, int amount, TransactionContext transaction) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        return Ints.saturatedCast(context.extract(ResourceConversion.ITEM.toVariant(resource), amount, FabricStorageResourceHandler.toFabric(transaction)));
    }
}
