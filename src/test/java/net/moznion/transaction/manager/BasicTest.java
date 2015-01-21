package net.moznion.transaction.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Test for basic txn handlings.
 * 
 * @author moznion
 *
 */
public class BasicTest extends TestBase {
	@Test
	public void basicTransaction() throws SQLException {
		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
			preparedStatement.executeUpdate();
		}
		txnManager.txnCommit();

		try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
			ResultSet rs = preparedStatement.executeQuery();
			rs.next();
			assertEquals(1, rs.getInt("id"));
			assertTrue(connection.getAutoCommit());
		}
	}

	@Test
	public void rollback() throws SQLException {
		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
			preparedStatement.executeUpdate();
		}
		txnManager.txnRollback();

		try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
			ResultSet rs = preparedStatement.executeQuery();
			assertTrue(!rs.next());
			assertTrue(connection.getAutoCommit());
		}
	}

	@Test
	public void basicTransactionWithNonAutoCommitMode() throws SQLException {
		connection.setAutoCommit(false);

		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
			preparedStatement.executeUpdate();
		}
		txnManager.txnCommit();

		try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
			ResultSet rs = preparedStatement.executeQuery();
			rs.next();
			assertEquals(1, rs.getInt("id"));
			assertTrue(!connection.getAutoCommit());
		}
	}

	@Test
	public void rollbackWithNonAutoCommitMode() throws SQLException {
		connection.setAutoCommit(false);

		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
			preparedStatement.executeUpdate();
		}
		txnManager.txnRollback();

		try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
			ResultSet rs = preparedStatement.executeQuery();
			assertTrue(!rs.next());
			assertTrue(!connection.getAutoCommit());
		}
	}

	@Test
	public void currentTransaction() throws SQLException {
		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
			preparedStatement.executeUpdate();
			TransactionTraceInfo ttrace = txnManager.getCurrentTransaction().get();
			assertEquals("net.moznion.transaction.manager.BasicTest", ttrace.getClassName());
			assertEquals("BasicTest.java", ttrace.getFileName());
			assertEquals("currentTransaction", ttrace.getMethodName());
			assertEquals(82, ttrace.getLineNumber());
			assertEquals(Thread.currentThread().getId(), ttrace.getThreadId());
		}
		txnManager.txnRollback();
	}

	@Test
	public void currentTransactionDoesNotExist() throws SQLException {
		TransactionManager txnManager = new TransactionManager(connection);
		assertTrue(!txnManager.getCurrentTransaction().isPresent());
	}
}
