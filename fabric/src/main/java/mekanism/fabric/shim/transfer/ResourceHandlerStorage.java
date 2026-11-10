package mekanism.fabric.shim.transfer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

/**
 * Exposes a NeoForge resource handler as a Fabric slotted storage.
 */
public class ResourceHandlerStorage<R extends Resource, V extends TransferVariant<?>> implements SlottedStorage<V> {

    private final ResourceHandler<R> handler;
    private final ResourceConversion<R, V> conversion;

    public ResourceHandlerStorage(ResourceHandler<R> handler, ResourceConversion<R, V> conversion) {
        this.handler = handler;
        this.conversion = conversion;
    }

    public ResourceHandler<R> getHandler() {
        return handler;
    }

    private int toNeo(long amount) {
        return (int) Math.min(Integer.MAX_VALUE, amount / conversion.scale());
    }

    @Override
    public long insert(V resource, long maxAmount, TransactionContext transaction) {
        int amount = toNeo(maxAmount);
        if (amount <= 0 || resource.isBlank()) {
            return 0;
        }
        return handler.insert(conversion.toResource(resource), amount, Transaction.wrap(transaction)) * conversion.scale();
    }

    @Override
    public long extract(V resource, long maxAmount, TransactionContext transaction) {
        int amount = toNeo(maxAmount);
        if (amount <= 0 || resource.isBlank()) {
            return 0;
        }
        return handler.extract(conversion.toResource(resource), amount, Transaction.wrap(transaction)) * conversion.scale();
    }

    @Override
    public int getSlotCount() {
        return handler.size();
    }

    @Override
    public SingleSlotStorage<V> getSlot(int slot) {
        return new Slot(slot);
    }

    @Override
    public Iterator<StorageView<V>> iterator() {
        int size = handler.size();
        List<StorageView<V>> slots = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            slots.add(new Slot(i));
        }
        return slots.iterator();
    }

    private final class Slot implements SingleSlotStorage<V> {

        private final int index;

        private Slot(int index) {
            this.index = index;
        }

        @Override
        public long insert(V resource, long maxAmount, TransactionContext transaction) {
            int amount = toNeo(maxAmount);
            if (amount <= 0 || resource.isBlank()) {
                return 0;
            }
            return handler.insert(index, conversion.toResource(resource), amount, Transaction.wrap(transaction)) * conversion.scale();
        }

        @Override
        public long extract(V resource, long maxAmount, TransactionContext transaction) {
            int amount = toNeo(maxAmount);
            if (amount <= 0 || resource.isBlank()) {
                return 0;
            }
            return handler.extract(index, conversion.toResource(resource), amount, Transaction.wrap(transaction)) * conversion.scale();
        }

        @Override
        public boolean isResourceBlank() {
            return handler.getResource(index).isEmpty();
        }

        @Override
        public V getResource() {
            return conversion.toVariant(handler.getResource(index));
        }

        @Override
        public long getAmount() {
            return handler.getAmountAsLong(index) * conversion.scale();
        }

        @Override
        public long getCapacity() {
            R resource = handler.getResource(index);
            return handler.getCapacityAsLong(index, resource) * conversion.scale();
        }
    }
}
