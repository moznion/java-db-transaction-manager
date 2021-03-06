package net.moznion.db.transaction.manager;

import lombok.Getter;
import net.moznion.db.transaction.manager.TransactionTraceInfo.Builder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The manager for transaction.
 *
 * @author moznion
 */
public class TransactionManager {
    private List<TransactionTraceInfo> activeTransactions;
    private int rollbackedInNestedTransaction = 0;

    @Getter
    private Boolean originalAutoCommitStatus = null;

    @Getter
    private final Connection connection;

    private final List<Runnable> endHooks;

    /**
     * Constructs a transaction manager.
     *
     * @param connection a connection of DB
     */
    public TransactionManager(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }

        this.connection = connection;
        activeTransactions = new ArrayList<>();
        endHooks = new ArrayList<>();
    }

    /**
     * Begins transaction.
     * <p>
     * This method backups automatically the status of auto commit mode when
     * this is called. The status will be turned back when transaction is end.
     *
     * @throws SQLException
     */
    public void txnBegin() throws SQLException {
        originalAutoCommitStatus = connection.getAutoCommit();
        txnBegin(originalAutoCommitStatus);
    }

    /**
     * Begins transaction with specified original auto commit status.
     * <p>
     * This method backups the status of auto commit mode which is specified as
     * an argument of this. The status will be turned back when transaction is
     * end.
     *
     * @param originalAutoCommitStatus original status of `auto commit` to restitute when all of transactions are finished.
     * @throws SQLException
     */
    public void txnBegin(boolean originalAutoCommitStatus) throws SQLException {
        if (activeTransactions.size() == 0) {
            this.originalAutoCommitStatus = originalAutoCommitStatus;
            connection.setAutoCommit(false); // Enable transaction
        }

        // `6` is really magical!! But it points the transaction stack
        Optional<StackTraceElement> maybeStackTraceElement = StackTracer
                .getStackTraceElement(6);

        Thread currentThread = Thread.currentThread();

        Builder ttiBuilder = TransactionTraceInfo.builder();
        ttiBuilder.threadId(currentThread.getId());

        if (maybeStackTraceElement.isPresent()) {
            StackTraceElement stackTraceElement = maybeStackTraceElement.get();
            ttiBuilder.className(stackTraceElement.getClassName())
                    .fileName(stackTraceElement.getFileName())
                    .methodName(stackTraceElement.getMethodName())
                    .lineNumber(stackTraceElement.getLineNumber());
        }

        activeTransactions.add(ttiBuilder.build());
    }

    /**
     * Commits the current transaction.
     *
     * @throws SQLException
     */
    public void txnCommit() throws SQLException {
        if (activeTransactions.size() <= 0) {
            return;
        }

        if (rollbackedInNestedTransaction > 0) {
            throw new AlreadyRollbackedException(
                    "Tried to commit but it had already rollbacked in nested transaction");
        }

        // remove a last item
        activeTransactions.remove(activeTransactions.size() - 1);

        if (activeTransactions.size() == 0) {
            connection.commit();
            txnEnd();
            endHooks.forEach(java.lang.Runnable::run);
        }
    }

    /**
     * Rollbacks the current transaction.
     *
     * @throws SQLException
     */
    public void txnRollback() throws SQLException {
        if (activeTransactions.size() <= 0) {
            return;
        }

        // remove a last item
        activeTransactions.remove(activeTransactions.size() - 1);

        if (activeTransactions.size() > 0) {
            rollbackedInNestedTransaction++;
        } else {
            connection.rollback();
            txnEnd();
        }
    }

    /**
     * Add an end hook for transaction.
     * <p>
     * Registered end hooks run only when all of transactions were succeeded.
     * Even one transaction was rollbacked, end hooks don't execute.
     *
     * @param r a processing for end hook
     */
    public void txnAddEndHook(Runnable r) {
        endHooks.add(r);
    }

    /**
     * Stack traced information of active transactions.
     *
     * @return a list of active transactions.
     */
    public List<TransactionTraceInfo> getActiveTransactions() {
        return activeTransactions;
    }

    /**
     * Stack traced information of current transactions.
     * <p>
     * If current transaction doesn't exist, it returns {@code Optional.empty()}
     * .
     * </p>
     *
     * @return a current activated transaction.
     */
    public Optional<TransactionTraceInfo> getCurrentTransaction() {
        if (activeTransactions.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(activeTransactions.get(activeTransactions.size() - 1));
    }

    private void txnEnd() throws SQLException {
        /*
         * turn back to original auto-commit mode
         */
        connection.setAutoCommit(originalAutoCommitStatus);

        activeTransactions = new ArrayList<>();
        rollbackedInNestedTransaction = 0;
    }
}
