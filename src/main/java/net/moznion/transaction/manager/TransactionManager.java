package net.moznion.transaction.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The manager for transaction.
 * 
 * @author moznion
 *
 */
public class TransactionManager {
	private final Connection connection;

	private List<TransactionTraceInfo> activeTransactions;
	private Boolean originalAutoCommitStatus = null;
	private int rollbackedInNestedTransaction = 0;

	/**
	 * Constructs a transaction manager.
	 * 
	 * @param connection
	 */
	public TransactionManager(Connection connection) {
		if (connection == null) {
			throw new IllegalArgumentException("connection must not be null");
		}

		this.connection = connection;
		activeTransactions = new ArrayList<>();
	}

	/**
	 * Begins transaction.
	 * 
	 * <p>
	 * This method backups automatically the status of auto commit mode when this is called.
	 * The status will be turned back when transaction is end.
	 * </p>
	 * 
	 * @throws SQLException
	 */
	public void txnBegin() throws SQLException {
		originalAutoCommitStatus = connection.getAutoCommit();
		txnBegin(originalAutoCommitStatus);
	}

	/**
	 * Begins transaction with specified original auto commit status.
	 * 
	 * <p>
	 * This method backups the status of auto commit mode which is specified as an argument of this.
	 * The status will be turned back when transaction is end.
	 * </p>
	 * 
	 * @param originalAutoCommitStatus
	 * @throws SQLException
	 */
	public void txnBegin(boolean originalAutoCommitStatus) throws SQLException {
		if (activeTransactions.size() == 0 && originalAutoCommitStatus) {
			connection.setAutoCommit(false); // Enable transaction
		}

		// `3` is magical, but it points the transaction stack
		Optional<StackTraceElement> maybeStackTraceElement = StackTracer.getStackTraceElement(3);

		Thread currentThread = Thread.currentThread();
		TransactionTraceInfo transactionTraceInfo = new TransactionTraceInfo(maybeStackTraceElement, currentThread.getId());
		activeTransactions.add(transactionTraceInfo);
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
			throw new AlreadyRollbackedException("Tried to commit but it had already rollbacked in nested transaction");
		}

		activeTransactions.remove(activeTransactions.size() - 1); // remove last item
		if (activeTransactions.size() == 0) {
			connection.commit();
			txnEnd();
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

		activeTransactions.remove(activeTransactions.size() - 1); // remove last item
		if (activeTransactions.size() > 0) {
			rollbackedInNestedTransaction++;
		} else {
			connection.rollback();
			txnEnd();
		}
	}

	/**
	 * Stack traced information of active transactions.
	 * 
	 * @return 
	 */
	public List<TransactionTraceInfo> getActiveTransactions() {
		return activeTransactions;
	}

	/**
	 * Stack traced information of current transactions.
	 * 
	 * <p>
	 * If current transaction doesn't exist, it returns {@code Optional.empty()}.
	 * </p>
	 * 
	 * @return
	 */
	public Optional<TransactionTraceInfo> getCurrentTransaction() {
		if (activeTransactions.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(activeTransactions.get(activeTransactions.size() - 1));
	}

	private void txnEnd() throws SQLException {
		connection.setAutoCommit(originalAutoCommitStatus); // turn back to original auto-commit mode

		activeTransactions = new ArrayList<>();
		rollbackedInNestedTransaction = 0;
	}

	/**
	 * The handler of a transaction manager which is scope (means try-with-resources statement) based.
	 * 
	 * @author moznion
	 *
	 */
	public class TransactionScope implements AutoCloseable {
		private boolean isActioned = false;

		/**
		 * Constructs a handler of a transaction manager with is scope based.
		 * 
		 * @throws SQLException
		 */
		public TransactionScope() throws SQLException {
			if (originalAutoCommitStatus == null) {
				originalAutoCommitStatus = connection.getAutoCommit();
			}
			txnBegin(originalAutoCommitStatus);
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

			txnCommit();
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

			txnRollback();
			isActioned = true;
		}

		@Override
		public void close() throws SQLException {
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
}
