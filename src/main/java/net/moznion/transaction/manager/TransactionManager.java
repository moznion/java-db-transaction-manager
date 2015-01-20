package net.moznion.transaction.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionManager {
	private final Connection connection;

	private List<TransactionTraceInfo> activeTransactions;
	private Savepoint savepoint = null;
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

		TransactionTraceInfo transactionTraceInfo = null;
		Thread currentThread = Thread.currentThread();
		StackTraceElement[] stackTraceElements = currentThread.getStackTrace();
		if (stackTraceElements.length > 3) {
			StackTraceElement caller = stackTraceElements[3]; // XXX 3 is magical, but it points caller stack
			transactionTraceInfo = TransactionTraceInfo.builder()
				.className(caller.getClassName())
				.fileName(caller.getFileName())
				.methodName(caller.getMethodName())
				.lineNumber(caller.getLineNumber())
				.threadId(currentThread.getId())
				.build();
		}
		activeTransactions.add(transactionTraceInfo);
	}

	public void txnCommit() throws SQLException {
		if (activeTransactions.size() <= 0) {
			return;
		}

		if (rollbackedInNestedTransaction > 0) {
			throw new RuntimeException(); // TODO
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
			if (savepoint != null) {
				connection.rollback(savepoint);
			} else {
				connection.rollback();
			}
			txnEnd();
		}
	}

	public void txnSave() throws SQLException {
		savepoint = connection.setSavepoint();
	}

	public void txnSave(String name) throws SQLException {
		if (name == null) {
			txnSave();
			return;
		}

		savepoint = connection.setSavepoint(name);
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
		savepoint = null;
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

		public void save() throws SQLException {
			txnSave();
		}

		public void save(String name) throws SQLException {
			txnSave(name);
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
