package mekanism.fabric.shim.transfer;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import team.reborn.energy.api.EnergyStorage;

/**
 * Exposes a NeoForge energy handler as a Team Reborn energy storage (one NeoForge energy unit is one Team Reborn energy unit).
 */
public record EnergyHandlerStorage(EnergyHandler handler) implements EnergyStorage {

    private static int toNeo(long amount) {
        return (int) Math.min(Integer.MAX_VALUE, amount);
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        return maxAmount <= 0 ? 0 : handler.insert(toNeo(maxAmount), Transaction.wrap(transaction));
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        return maxAmount <= 0 ? 0 : handler.extract(toNeo(maxAmount), Transaction.wrap(transaction));
    }

    @Override
    public long getAmount() {
        return handler.getAmountAsLong();
    }

    @Override
    public long getCapacity() {
        return handler.getCapacityAsLong();
    }
}
