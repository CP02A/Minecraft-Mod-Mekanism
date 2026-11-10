package mekanism.fabric.shim.transfer;

import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a NeoForge {@link ItemAccess} as a Fabric {@link ContainerItemContext}. NeoForge's item access doesn't distinguish between its main slot
 * and overflow, so insertions into the main slot of a matching resource may overflow into wherever the access puts them.
 */
public record ItemAccessContext(ItemAccess access) implements ContainerItemContext {

    public static ContainerItemContext of(ItemStack stack, @Nullable ItemAccess access) {
        if (access == null) {
            return ContainerItemContext.withConstant(stack);
        }
        return access instanceof ContextItemAccess(ContainerItemContext context) ? context : new ItemAccessContext(access);
    }

    private static int toNeo(long amount) {
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    @Override
    public long insert(ItemVariant itemVariant, long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0 || itemVariant.isBlank()) {
            return 0;
        }
        return access.insert(ResourceConversion.ITEM.toResource(itemVariant), toNeo(maxAmount), Transaction.wrap(transaction));
    }

    @Override
    public long extract(ItemVariant itemVariant, long maxAmount, TransactionContext transaction) {
        if (maxAmount <= 0 || itemVariant.isBlank()) {
            return 0;
        }
        return access.extract(ResourceConversion.ITEM.toResource(itemVariant), toNeo(maxAmount), Transaction.wrap(transaction));
    }

    @Override
    public SingleSlotStorage<ItemVariant> getMainSlot() {
        return new MainSlot();
    }

    @Override
    public long insertOverflow(ItemVariant itemVariant, long maxAmount, TransactionContext transaction) {
        return insert(itemVariant, maxAmount, transaction);
    }

    @Override
    public List<SingleSlotStorage<ItemVariant>> getAdditionalSlots() {
        return List.of();
    }

    private final class MainSlot implements SingleSlotStorage<ItemVariant> {

        @Override
        public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            ItemResource current = access.getResource();
            if (!current.isEmpty() && !ResourceConversion.ITEM.toResource(resource).equals(current)) {
                return 0;
            }
            return ItemAccessContext.this.insert(resource, maxAmount, transaction);
        }

        @Override
        public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
            return ItemAccessContext.this.extract(resource, maxAmount, transaction);
        }

        @Override
        public boolean isResourceBlank() {
            return access.getResource().isEmpty();
        }

        @Override
        public ItemVariant getResource() {
            return ResourceConversion.ITEM.toVariant(access.getResource());
        }

        @Override
        public long getAmount() {
            return access.getAmount();
        }

        @Override
        public long getCapacity() {
            ItemResource resource = access.getResource();
            return resource.isEmpty() ? ItemStack.EMPTY.getMaxStackSize() : resource.getMaxStackSize();
        }
    }
}
