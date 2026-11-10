package mekanism.fabric.shim.transfer;

import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.resource.Resource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * Exposes a Fabric storage as a NeoForge resource handler. Storages that aren't slotted are exposed as a single slot handler.
 */
public class FabricStorageResourceHandler<R extends Resource, V extends TransferVariant<?>> implements ResourceHandler<R> {

    private final Storage<V> storage;
    private final ResourceConversion<R, V> conversion;

    public FabricStorageResourceHandler(Storage<V> storage, ResourceConversion<R, V> conversion) {
        this.storage = storage;
        this.conversion = conversion;
    }

    public Storage<V> getStorage() {
        return storage;
    }

    static net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext toFabric(TransactionContext transaction) {
        return (net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext) ((net.neoforged.neoforge.transfer.transaction.Transaction) transaction).nativeHandle();
    }

    @Nullable
    private StorageView<V> view(int index) {
        if (storage instanceof SlottedStorage<V> slotted) {
            return index < slotted.getSlotCount() ? slotted.getSlot(index) : null;
        }
        for (StorageView<V> view : storage.nonEmptyViews()) {
            return view;
        }
        return null;
    }

    @Override
    public int size() {
        return storage instanceof SlottedStorage<V> slotted ? slotted.getSlotCount() : 1;
    }

    @Override
    public R getResource(int index) {
        StorageView<V> view = view(index);
        return view == null || view.isResourceBlank() ? conversion.emptyResource() : conversion.toResource(view.getResource());
    }

    @Override
    public long getAmountAsLong(int index) {
        StorageView<V> view = view(index);
        return view == null || view.isResourceBlank() ? 0 : view.getAmount() / conversion.scale();
    }

    @Override
    public long getCapacityAsLong(int index, R resource) {
        StorageView<V> view = view(index);
        return view == null ? 0 : view.getCapacity() / conversion.scale();
    }

    @Override
    public boolean isValid(int index, R resource) {
        //Fabric storages don't expose validity checks
        return storage.supportsInsertion();
    }

    @Override
    public int insert(int index, R resource, int amount, TransactionContext transaction) {
        if (storage instanceof SlottedStorage<V> slotted) {
            return index < slotted.getSlotCount() ? transfer(slotted.getSlot(index), resource, amount, transaction, true) : 0;
        }
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(R resource, int amount, TransactionContext transaction) {
        return transfer(storage, resource, amount, transaction, true);
    }

    @Override
    public int extract(int index, R resource, int amount, TransactionContext transaction) {
        if (storage instanceof SlottedStorage<V> slotted) {
            return index < slotted.getSlotCount() ? transfer(slotted.getSlot(index), resource, amount, transaction, false) : 0;
        }
        return extract(resource, amount, transaction);
    }

    @Override
    public int extract(R resource, int amount, TransactionContext transaction) {
        return transfer(storage, resource, amount, transaction, false);
    }

    /**
     * Transfers whole NeoForge units only: if the Fabric storage moves a partial unit, the transfer is redone with the amount rounded down.
     */
    private int transfer(Storage<V> target, R resource, int amount, TransactionContext transaction, boolean insert) {
        if (amount <= 0 || resource.isEmpty()) {
            return 0;
        }
        V variant = conversion.toVariant(resource);
        long scale = conversion.scale();
        net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext fabric = toFabric(transaction);
        long moved;
        try (Transaction nested = fabric.openNested()) {
            moved = insert ? target.insert(variant, amount * scale, nested) : target.extract(variant, amount * scale, nested);
            if (moved % scale == 0) {
                nested.commit();
                return (int) (moved / scale);
            }
        }
        long whole = moved / scale * scale;
        if (whole == 0) {
            return 0;
        }
        try (Transaction nested = fabric.openNested()) {
            moved = insert ? target.insert(variant, whole, nested) : target.extract(variant, whole, nested);
            if (moved == whole) {
                nested.commit();
                return (int) (whole / scale);
            }
        }
        return 0;
    }
}
