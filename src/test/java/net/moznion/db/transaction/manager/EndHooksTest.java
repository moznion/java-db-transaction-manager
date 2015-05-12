package net.moznion.db.transaction.manager;

import net.moznion.capture.output.stream.StdoutCapturer;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public class EndHooksTest extends TestBase {
    @Test
    public void shouldEndHooksRun() throws SQLException {
        TransactionManager txnManager = new TransactionManager(connection);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try (StdoutCapturer ignored = new StdoutCapturer(stdout)) {
            txnManager.txnAddEndHook(() -> System.out.println("no1"));
            txnManager.txnAddEndHook(() -> System.out.println("no2"));
            txnManager.txnBegin();
            txnManager.txnCommit();
        }
        assertEquals("no1\nno2\n", stdout.toString());
    }

    @Test
    public void shouldNotEndHooksRun() throws SQLException {
        TransactionManager txnManager = new TransactionManager(connection);

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        try (StdoutCapturer ignored = new StdoutCapturer(stdout)) {
            txnManager.txnAddEndHook(() -> System.out.println("no1"));
            txnManager.txnAddEndHook(() -> System.out.println("no2"));
            txnManager.txnBegin();
            txnManager.txnRollback();
        }
        assertEquals("", stdout.toString());
    }
}
