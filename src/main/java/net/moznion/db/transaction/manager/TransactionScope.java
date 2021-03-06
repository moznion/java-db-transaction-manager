package net.moznion.db.transaction.manager;

import java.sql.SQLException;
import java.util.List;

/**
 * The handler of a transaction manager which is scope (means try-with-resources statement) based.
 *
 * @author moznion
 */
public class TransactionScope implements AutoCloseable {
    private boolean isActioned = false;
    private final TransactionManager transactionManager;

    /**
     * Constructs a handler of a transaction manager with is scope based.
     *
     * @throws SQLException
     */
    public TransactionScope(TransactionManager transactionManager) throws SQLException {
        if (transactionManager == null) {
            throw new IllegalArgumentException("transactionManager must not be null");
        }
        this.transactionManager = transactionManager;

        Boolean originalAutoCommitStatus = transactionManager.getOriginalAutoCommitStatus();
        if (originalAutoCommitStatus == null) {
            originalAutoCommitStatus = transactionManager.getConnection().getAutoCommit();
        }
        transactionManager.txnBegin(originalAutoCommitStatus);
    }

    /**
     * Commits the current transaction.
     *
     * @throws SQLException
     */
    public void commit() throws SQLException {
        if (isActioned) {
            return; // do not run twice
        }

        transactionManager.txnCommit();
        isActioned = true;
    }

    /**
     * Rollbacks the current transaction.
     *
     * @throws SQLException
     */
    public void rollback() throws SQLException {
        if (isActioned) {
            return; // do not run twice
        }

        transactionManager.txnRollback();
        isActioned = true;
    }

    /**
     * Add an end hook for transaction.
     * <p>
     * Registered end hooks run only when all of transactions were succeeded.
     * Even one transaction was rollbacked, end hooks don't execute.
     *
     * @param r a processing for end hook
     */
    public void addEndHook(Runnable r) {
        transactionManager.txnAddEndHook(r);
    }

    @Override
    public void close() throws SQLException {
        List<TransactionTraceInfo> activeTransactions = transactionManager.getActiveTransactions();
        if (activeTransactions.isEmpty()) {
            return;
        }

        TransactionTraceInfo currentTransactionTraceInfo = activeTransactions.get(activeTransactions.size() - 1);
        if (Thread.currentThread().getId() != currentTransactionTraceInfo.getThreadId()) {
            return;
        }

        if (!isActioned) {
            rollback();
        }
    }
}
