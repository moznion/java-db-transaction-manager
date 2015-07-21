package net.moznion.db.transaction.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Test for scope based transaction management.
 *
 * @author moznion
 */
public class ScopeTest extends TestBase {
    @Test
    public void basicScopeTransaction() throws SQLException {
        try (TransactionScope txn = new TransactionScope(new TransactionManager(connection))) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
                preparedStatement.executeUpdate();
            }
            txn.commit();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            assertEquals(1, rs.getInt("id"));
            assertTrue(connection.getAutoCommit());
        }
    }

    @Test
    public void scopeRollback() throws SQLException {
        try (TransactionScope txn = new TransactionScope(new TransactionManager(connection))) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
                preparedStatement.executeUpdate();
            }
            txn.rollback();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
            ResultSet rs = preparedStatement.executeQuery();
            assertTrue(!rs.next());
            assertTrue(connection.getAutoCommit());
        }
    }

    @Test
    public void autoRollbackByTryWithResources() throws SQLException {
        try (TransactionScope txn = new TransactionScope(new TransactionManager(connection))) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
                preparedStatement.executeUpdate();
            }
        } // auto rollback if it reaches here.

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
            ResultSet rs = preparedStatement.executeQuery();
            assertTrue(!rs.next());
            assertTrue(connection.getAutoCommit());
        }
    }

    @Test
    public void nestedScopeWithRollbackAndRollback() throws SQLException {
        TransactionManager transactionManager = new TransactionManager(connection);

        try (TransactionScope txn1 = new TransactionScope(transactionManager)) {
            try (TransactionScope txn2 = new TransactionScope(transactionManager)) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
                    preparedStatement.executeUpdate();
                }
                txn2.rollback();
            }
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')")) {
                preparedStatement.executeUpdate();
            }
            txn1.rollback();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
            ResultSet rs = preparedStatement.executeQuery();
            assertTrue(!rs.next());
            assertTrue(connection.getAutoCommit());
        }
    }

    @Test
    public void nestedScopeWithCommitAndRollback() throws SQLException {
        TransactionManager transactionManager = new TransactionManager(connection);

        try (TransactionScope txn1 = new TransactionScope(transactionManager)) {
            try (TransactionScope txn2 = new TransactionScope(transactionManager)) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
                    preparedStatement.executeUpdate();
                }
                txn2.commit();
            }
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')")) {
                preparedStatement.executeUpdate();
            }
            txn1.rollback();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
            ResultSet rs = preparedStatement.executeQuery();
            assertTrue(!rs.next());
            assertTrue(connection.getAutoCommit());
        }
    }

    @Test(expected = AlreadyRollbackedException.class)
    public void nestedScopeWithRollbackAndCommit() throws SQLException {
        TransactionManager transactionManager = new TransactionManager(connection);

        try (TransactionScope txn1 = new TransactionScope(transactionManager)) {
            try (TransactionScope txn2 = new TransactionScope(transactionManager)) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
                    preparedStatement.executeUpdate();
                }
                txn2.rollback();
            }
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')")) {
                preparedStatement.executeUpdate();
            }
            txn1.commit();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
            ResultSet rs = preparedStatement.executeQuery();
            assertTrue(!rs.next());
            assertTrue(connection.getAutoCommit());
        }
    }

    @Test
    public void nestedScopeWithCommitAndCommit() throws SQLException {
        TransactionManager transactionManager = new TransactionManager(connection);

        try (TransactionScope txn1 = new TransactionScope(transactionManager)) {
            try (TransactionScope txn2 = new TransactionScope(transactionManager)) {
                try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
                    preparedStatement.executeUpdate();
                }
                txn2.commit();
            }
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (2, 'qux')")) {
                preparedStatement.executeUpdate();
            }
            txn1.commit();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
            ResultSet rs = preparedStatement.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("id"));
            assertEquals("baz", rs.getString("var"));

            assertTrue(rs.next());
            assertEquals(2, rs.getInt("id"));
            assertEquals("qux", rs.getString("var"));

            assertTrue(!rs.next());

            assertTrue(connection.getAutoCommit());
        }
    }

    @Test
    public void basicScopeTransactionWithNonAutoCommitMode() throws SQLException {
        connection.setAutoCommit(false);

        try (TransactionScope txn = new TransactionScope(new TransactionManager(connection))) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
                preparedStatement.executeUpdate();
            }
            txn.commit();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            assertEquals(1, rs.getInt("id"));
            assertTrue(!connection.getAutoCommit());
        }
    }

    @Test
    public void scopeRollbackWithNonAutoCommitMode() throws SQLException {
        connection.setAutoCommit(false);

        try (TransactionScope txn = new TransactionScope(new TransactionManager(connection))) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO foo (id, var) VALUES (1, 'baz')")) {
                preparedStatement.executeUpdate();
            }
            txn.rollback();
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM foo")) {
            ResultSet rs = preparedStatement.executeQuery();
            assertTrue(!rs.next());
            assertTrue(!connection.getAutoCommit());
        }
    }

    @SuppressWarnings("resource")
    @Test(expected = IllegalArgumentException.class)
    public void gaveNullAsConnection() throws SQLException {
        new TransactionScope(null);
    }
}
