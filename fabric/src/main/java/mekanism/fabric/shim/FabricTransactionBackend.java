package mekanism.fabric.shim;

import java.util.function.Consumer;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.neoforged.neoforge.shim.TransactionBackend;
import org.jspecify.annotations.Nullable;

/**
 * Backs the shim's (NeoForge style) transactions with Fabric's transactions, so that transactions opened by Mekanism and by other Fabric mods are
 * the same and can be nested in each other.
 */
public class FabricTransactionBackend implements TransactionBackend {

    @Override
    public Object openRoot() {
        return Transaction.openOuter();
    }

    @Override
    public Object openNested(Object parent) {
        return ((TransactionContext) parent).openNested();
    }

    @Override
    public void commit(Object handle) {
        ((Transaction) handle).commit();
    }

    @Override
    public void close(Object handle) {
        ((Transaction) handle).close();
    }

    @Override
    public int depth(Object handle) {
        return ((TransactionContext) handle).nestingDepth();
    }

    @Override
    public Object getOpenTransaction(Object handle, int depth) {
        return ((TransactionContext) handle).getOpenTransaction(depth);
    }

    @Override
    public void addCloseCallback(Object handle, Consumer<Boolean> onClose) {
        ((TransactionContext) handle).addCloseCallback((transaction, result) -> onClose.accept(result.wasAborted()));
    }

    @Override
    public void addRootCloseCallback(Object handle, Consumer<Boolean> afterRootClose) {
        ((TransactionContext) handle).addOuterCloseCallback(result -> afterRootClose.accept(result.wasCommitted()));
    }

    @Nullable
    @Override
    public Object currentOpen() {
        return Transaction.getCurrentUnsafe();
    }

    @Override
    public net.neoforged.neoforge.transfer.transaction.Transaction.Lifecycle lifecycle() {
        return switch (Transaction.getLifecycle()) {
            case NONE -> net.neoforged.neoforge.transfer.transaction.Transaction.Lifecycle.NONE;
            case OPEN -> net.neoforged.neoforge.transfer.transaction.Transaction.Lifecycle.OPEN;
            case CLOSING -> net.neoforged.neoforge.transfer.transaction.Transaction.Lifecycle.CLOSING;
            case OUTER_CLOSING -> net.neoforged.neoforge.transfer.transaction.Transaction.Lifecycle.ROOT_CLOSING;
        };
    }
}
