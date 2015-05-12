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
        this.transactionManager = transactionManager;
        if (transactionManager == null) {
            throw new IllegalArgumentException("transactionManager must not be null");
        }

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
