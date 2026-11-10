package mekanism.fabric.shim.transfer;

import com.google.common.primitives.Ints;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import team.reborn.energy.api.EnergyStorage;

/**
 * Exposes a Team Reborn energy storage as a NeoForge energy handler.
 */
public record StorageEnergyHandler(EnergyStorage storage) implements EnergyHandler {

    @Override
    public long getAmountAsLong() {
        return storage.getAmount();
    }

    @Override
    public long getCapacityAsLong() {
        return storage.getCapacity();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        if (amount <= 0 || !storage.supportsInsertion()) {
            return 0;
        }
        return Ints.saturatedCast(storage.insert(amount, FabricStorageResourceHandler.toFabric(transaction)));
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        if (amount <= 0 || !storage.supportsExtraction()) {
            return 0;
        }
        return Ints.saturatedCast(storage.extract(amount, FabricStorageResourceHandler.toFabric(transaction)));
    }
}
