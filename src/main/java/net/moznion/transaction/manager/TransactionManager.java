package net.moznion.transaction.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionManager {
	private final Connection connection;

	private List<TransactionTraceInfo> activeTransactions;
	private Boolean originalAutoCommitStatus = null;
	private int rollbackedInNestedTransaction = 0;

	public TransactionManager(Connection connection) {
		if (connection == null) {
			throw new IllegalArgumentException("connection must not be null");
		}

		this.connection = connection;
		activeTransactions = new ArrayList<>();
	}

	public void txnBegin() throws SQLException {
		originalAutoCommitStatus = connection.getAutoCommit();
		txnBegin(originalAutoCommitStatus);
	}

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

	public List<TransactionTraceInfo> getActiveTransactions() {
		return activeTransactions;
	}

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

	public class TransactionScope implements AutoCloseable {
		private boolean isActioned = false;

		public TransactionScope() throws SQLException {
			if (originalAutoCommitStatus == null) {
				originalAutoCommitStatus = connection.getAutoCommit();
			}
			txnBegin(originalAutoCommitStatus);
		}

		public void commit() throws SQLException {
			if (isActioned) {
				return; // do not run twice
			}

			txnCommit();
			isActioned = true;
		}

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
