package net.moznion.transaction.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.moznion.transaction.manager.TransactionManager.TransactionScope;

import org.junit.Test;

public class ScopeTest extends TestBase {
	@Test
	public void basicScopeTransaction() throws SQLException {
		try (TransactionScope txn = new TransactionManager(connection).new TransactionScope()) {
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
			txn.commit();
		}

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		rs.next();
		assertEquals(1, rs.getInt("id"));
		assertTrue(connection.getAutoCommit());
	}

	@Test
	public void scopeRollback() throws SQLException {
		try (TransactionScope txn = new TransactionManager(connection).new TransactionScope()) {
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
			txn.rollback();
		}

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(!rs.next());
		assertTrue(connection.getAutoCommit());
	}

	@Test
	public void autoRollbackByTryWithResources() throws SQLException {
		try (TransactionScope txn = new TransactionManager(connection).new TransactionScope()) {
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
		} // auto rollback if it reaches here.

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(!rs.next());
		assertTrue(connection.getAutoCommit());
	}

	@Test
	public void nestedScopeWithRollback_Rollback() throws SQLException {
		TransactionManager transactionManager = new TransactionManager(connection);

		try (TransactionScope txn1 = transactionManager.new TransactionScope()) {
			try (TransactionScope txn2 = transactionManager.new TransactionScope()) {
				connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
				txn2.rollback();
			}
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')").executeUpdate();
			txn1.rollback();
		}

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(!rs.next());
		assertTrue(connection.getAutoCommit());
	}

	@Test
	public void nestedScopeWithCommit_Rollback() throws SQLException {
		TransactionManager transactionManager = new TransactionManager(connection);

		try (TransactionScope txn1 = transactionManager.new TransactionScope()) {
			try (TransactionScope txn2 = transactionManager.new TransactionScope()) {
				connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
				txn2.commit();
			}
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')").executeUpdate();
			txn1.rollback();
		}

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(!rs.next());
		assertTrue(connection.getAutoCommit());
	}

	@Test
	public void nestedScopeWithRollback_Commit() throws SQLException {
		TransactionManager transactionManager = new TransactionManager(connection);

		try (TransactionScope txn1 = transactionManager.new TransactionScope()) {
			try (TransactionScope txn2 = transactionManager.new TransactionScope()) {
				connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
				txn2.rollback();
			}
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')").executeUpdate();
			txn1.commit();
		} catch (RuntimeException e) {
			// TODO
		}

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(!rs.next());
		assertTrue(connection.getAutoCommit());
	}

	@Test
	public void nestedScopeWithCommit_Commit() throws SQLException {
		TransactionManager transactionManager = new TransactionManager(connection);

		try (TransactionScope txn1 = transactionManager.new TransactionScope()) {
			try (TransactionScope txn2 = transactionManager.new TransactionScope()) {
				connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
				txn2.commit();
			}
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')").executeUpdate();
			txn1.commit();
		} catch (RuntimeException e) {
			// TODO
		}

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();

		assertTrue(rs.next());
		assertEquals(1, rs.getInt("id"));
		assertEquals("baz", rs.getString("var"));

		assertTrue(rs.next());
		assertEquals(2, rs.getInt("id"));
		assertEquals("qux", rs.getString("var"));

		assertTrue(!rs.next());

		assertTrue(connection.getAutoCommit());
	}

	@Test
	public void basicScopeTransactionWithNonAutoCommitMode() throws SQLException {
		connection.setAutoCommit(false);

		try (TransactionScope txn = new TransactionManager(connection).new TransactionScope()) {
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
			txn.commit();
		}

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		rs.next();
		assertEquals(1, rs.getInt("id"));
		assertTrue(!connection.getAutoCommit());
	}

	@Test
	public void scopeRollbackWithNonAutoCommitMode() throws SQLException {
		connection.setAutoCommit(false);

		try (TransactionScope txn = new TransactionManager(connection).new TransactionScope()) {
			connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')").executeUpdate();
			txn.rollback();
		}

		ResultSet rs = connection.prepareStatement("SELECT * FROM foo").executeQuery();
		assertTrue(!rs.next());
		assertTrue(!connection.getAutoCommit());
	}
}
