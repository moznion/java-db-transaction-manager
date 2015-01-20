package net.moznion.transaction.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.Test;

public class BasicTest extends TestBase {
	@Test
	public void basicTransaction() throws SQLException {
		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		{
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
		}
		txnManager.txnCommit();

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		rs.next();
		assertEquals(1, rs.getInt("id"));
		assertTrue(connection.getAutoCommit());
	}

	@Test
	public void rollback() throws SQLException {
		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		{
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
		}
		txnManager.txnRollback();

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(!rs.next());
		assertTrue(connection.getAutoCommit());
	}

	@Test
	public void basicTransactionWithNonAutoCommitMode() throws SQLException {
		connection.setAutoCommit(false);

		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		{
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
		}
		txnManager.txnCommit();

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		rs.next();
		assertEquals(1, rs.getInt("id"));
		assertTrue(!connection.getAutoCommit());
	}

	@Test
	public void rollbackWithNonAutoCommitMode() throws SQLException {
		connection.setAutoCommit(false);

		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		{
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
		}
		txnManager.txnRollback();

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(!rs.next());
		assertTrue(!connection.getAutoCommit());
	}

	@Test
	public void currentTransaction() throws SQLException {
		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		{
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
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

	@Test
	public void rollbackWithAnonymousSavepoint() throws SQLException {
		connection.setAutoCommit(false);

		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		{
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
			txnManager.txnSave();
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')").executeUpdate();
		}
		txnManager.txnRollback();

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(rs.next());
		assertEquals(1, rs.getInt("id"));
		assertEquals("baz", rs.getString("var"));

		assertTrue(!rs.next());
	}

	@Test
	public void rollbackWithNamedSavepoint() throws SQLException {
		connection.setAutoCommit(false);

		TransactionManager txnManager = new TransactionManager(connection);

		txnManager.txnBegin();
		{
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
			txnManager.txnSave("SAVE ME!");
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')").executeUpdate();
		}
		txnManager.txnRollback();

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(rs.next());
		assertEquals(1, rs.getInt("id"));
		assertEquals("baz", rs.getString("var"));

		assertTrue(!rs.next());
	}
}
