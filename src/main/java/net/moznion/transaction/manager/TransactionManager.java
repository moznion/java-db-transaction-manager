package net.moznion.transaction.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;

public class TransactionManager {
	private final Connection connection;
	private final List<Savepoint> savepoints;
	private Boolean originalAutoCommitStatus = null;
	private int activeTransactionCount = 0;
	private int rollbackedInNestedTransaction = 0;

	public TransactionManager(Connection connection) {
		this.connection = connection;
		savepoints = new ArrayList<>();
	}

	public void txnBegin() throws SQLException {
		originalAutoCommitStatus = connection.getAutoCommit();
		txnBegin(originalAutoCommitStatus);
	}

	public void txnBegin(boolean originalAutoCommitStatus) throws SQLException {
		if (originalAutoCommitStatus) {
			connection.setAutoCommit(false); // Enable transaction
		}
		activeTransactionCount++;
	}

	public void txnCommit() throws SQLException {
		if (activeTransactionCount <= 0) {
			return;
		}

		if (rollbackedInNestedTransaction > 0) {
			throw new RuntimeException(); // TODO
		}

		activeTransactionCount--;
		if (activeTransactionCount == 0) {
			connection.commit();
			txnEnd();
		}
	}

	public void txnRollback() throws SQLException {
		//		if (savepoints.isEmpty()) {
		//			connection.rollback();
		//		} else {
		//			connection.rollback(savepoints.remove(0));
		//			connection.commit();
		//		}
		if (activeTransactionCount <= 0) {
			return;
		}

		activeTransactionCount--;

		if (activeTransactionCount > 0) {
			rollbackedInNestedTransaction++;
		} else {
			connection.rollback();
			txnEnd();
		}
	}

	private void txnEnd() throws SQLException {
		// turn back to original auto-commit mode
		connection.setAutoCommit(originalAutoCommitStatus);

		activeTransactionCount = 0;
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
			if (!isActioned) {
				rollback();
			}
		}
	}
}
