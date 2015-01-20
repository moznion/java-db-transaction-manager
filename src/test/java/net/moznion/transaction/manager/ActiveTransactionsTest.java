package net.moznion.transaction.manager;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.List;

import net.moznion.transaction.manager.TransactionManager.TransactionScope;

import org.junit.Test;

public class ActiveTransactionsTest extends TestBase {
	@Test
	public void proveActiveTransactions() throws SQLException {
		final long currentThreadId = Thread.currentThread().getId();
		final String filename = "ActiveTransactionsTest.java";

		TransactionManager txnManager = new TransactionManager(connection);

		assertEquals(0, txnManager.getActiveTransactions().size());

		try (TransactionScope txn1 = txnManager.new TransactionScope()) {
			{
				List<TransactionTrace> activeTransactions = txnManager.getActiveTransactions();
				assertEquals(1, activeTransactions.size());

				TransactionTrace got = activeTransactions.get(0);
				assertEquals("net.moznion.transaction.manager.ActiveTransactionsTest", got.getClassName());
				assertEquals(filename, got.getFileName());
				assertEquals("proveActiveTransactions", got.getMethodName());
				assertEquals(22, got.getLineNumber());
				assertEquals(currentThreadId, got.getThreadId());
			}

			try (TransactionScope txn2 = txnManager.new TransactionScope()) {
				{
					List<TransactionTrace> activeTransactions = txnManager.getActiveTransactions();
					assertEquals(2, activeTransactions.size());

					TransactionTrace got = activeTransactions.get(1);
					assertEquals("net.moznion.transaction.manager.ActiveTransactionsTest", got.getClassName());
					assertEquals(filename, got.getFileName());
					assertEquals("proveActiveTransactions", got.getMethodName());
					assertEquals(35, got.getLineNumber());
					assertEquals(currentThreadId, got.getThreadId());

					txn2.commit();
				}
			}

			{
				List<TransactionTrace> activeTransactions = txnManager.getActiveTransactions();
				assertEquals(1, activeTransactions.size());

				TransactionTrace got = activeTransactions.get(0);
				assertEquals("net.moznion.transaction.manager.ActiveTransactionsTest", got.getClassName());
				assertEquals(filename, got.getFileName());
				assertEquals("proveActiveTransactions", got.getMethodName());
				assertEquals(22, got.getLineNumber());
				assertEquals(currentThreadId, got.getThreadId());
			}

			txnManager.txnBegin();
			{
				List<TransactionTrace> activeTransactions = txnManager.getActiveTransactions();
				assertEquals(2, activeTransactions.size());

				TransactionTrace got = activeTransactions.get(1);
				assertEquals("net.moznion.transaction.manager.ActiveTransactionsTest", got.getClassName());
				assertEquals(filename, got.getFileName());
				assertEquals("proveActiveTransactions", got.getMethodName());
				assertEquals(63, got.getLineNumber());
				assertEquals(currentThreadId, got.getThreadId());
			}
			txnManager.txnCommit();

			txnManager.txnBegin();
			{
				List<TransactionTrace> activeTransactions = txnManager.getActiveTransactions();
				assertEquals(2, activeTransactions.size());

				TransactionTrace got = activeTransactions.get(1);
				assertEquals("net.moznion.transaction.manager.ActiveTransactionsTest", got.getClassName());
				assertEquals(filename, got.getFileName());
				assertEquals("proveActiveTransactions", got.getMethodName());
				assertEquals(77, got.getLineNumber());
				assertEquals(currentThreadId, got.getThreadId());
			}
			txnManager.txnRollback();
		} // auto rollback

		assertEquals(0, txnManager.getActiveTransactions().size());
	}
}
