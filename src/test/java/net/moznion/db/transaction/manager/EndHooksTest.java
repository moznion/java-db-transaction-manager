package net.moznion.db.transaction.manager;

import net.moznion.capture.output.stream.StdoutCapturer;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class EndHooksTest extends TestBase {
    @Test
    public void shouldEndHooksRun() throws SQLException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try (StdoutCapturer ignored = new StdoutCapturer(stdout)) {
            TransactionManager txnManager = new TransactionManager(connection);
            txnManager.txnAddEndHook(() -> System.out.println("no1"));
            txnManager.txnAddEndHook(() -> System.out.println("no2"));
            txnManager.txnBegin();
            txnManager.txnCommit();
        }
        assertEquals("no1\nno2\n", stdout.toString());
    }

    @Test
    public void shouldNotEndHooksRun() throws SQLException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try (StdoutCapturer ignored = new StdoutCapturer(stdout)) {
            TransactionManager txnManager = new TransactionManager(connection);
            txnManager.txnAddEndHook(() -> System.out.println("no1"));
            txnManager.txnAddEndHook(() -> System.out.println("no2"));
            txnManager.txnBegin();
            txnManager.txnRollback();
        }
        assertEquals("", stdout.toString());
    }

    @Test
    public void shouldEndHooksRunWithScope() throws SQLException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try (StdoutCapturer ignored = new StdoutCapturer(stdout)) {
            TransactionManager txnManager = new TransactionManager(connection);
            try (TransactionScope transactionScope = new TransactionScope(txnManager)) {
                transactionScope.addEndHook(() -> System.out.println("no1"));
                transactionScope.addEndHook(() -> System.out.println("no2"));
                transactionScope.commit();
            }
        }
        assertEquals("no1\nno2\n", stdout.toString());
    }

    @Test
    public void shouldNotEndHooksRunWithScope() throws SQLException {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try (StdoutCapturer ignored = new StdoutCapturer(stdout)) {
            TransactionManager txnManager = new TransactionManager(connection);
            try (TransactionScope transactionScope = new TransactionScope(txnManager)) {
                transactionScope.addEndHook(() -> System.out.println("no1"));
                transactionScope.addEndHook(() -> System.out.println("no2"));
            }
        }
        assertEquals("", stdout.toString());
    }
}
