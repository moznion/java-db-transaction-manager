package net.moznion.transaction.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class TransactionManager {
	private final Connection connection;

	private Boolean originalAutoCommitStatus = null;
	private int rollbackedInNestedTransaction = 0;

	@Getter
	private List<TransactionTrace> activeTransactions;

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

		TransactionTrace transactionTrace = null;
		Thread currentThread = Thread.currentThread();
		StackTraceElement[] stackTraceElements = currentThread.getStackTrace();
		if (stackTraceElements.length > 3) {
			StackTraceElement caller = stackTraceElements[3]; // XXX 3 is magical, but it points caller stack
			transactionTrace = TransactionTrace.builder()
				.className(caller.getClassName())
				.fileName(caller.getFileName())
				.methodName(caller.getMethodName())
				.lineNumber(caller.getLineNumber())
				.threadId(currentThread.getId())
				.build();
		}
		activeTransactions.add(transactionTrace);
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
			connection.rollback();
			txnEnd();
		}
	}

	private void txnEnd() throws SQLException {
		// turn back to original auto-commit mode
		connection.setAutoCommit(originalAutoCommitStatus);

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
			int numOfActiveTransactions = activeTransactions.size();
			if (numOfActiveTransactions <= 0) {
				return;
			}

			TransactionTrace currentTransactionTrace = activeTransactions.get(numOfActiveTransactions - 1);
			if (Thread.currentThread().getId() != currentTransactionTrace.getThreadId()) {
				return;
			}

			if (!isActioned) {
				rollback();
			}
		}
	}
}
