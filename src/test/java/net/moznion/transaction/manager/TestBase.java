package net.moznion.transaction.manager;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Base class for Tests.
 * 
 * @author moznion
 *
 */
public class TestBase {
	private static final String BASE_URL = "jdbc:mysql://localhost";
	private static final String USER = "root";
	private static final String PASSWORD = "";
	private static String testDBName;

	@SuppressFBWarnings
	protected static Connection connection;

	@BeforeClass
	@SuppressFBWarnings
	public static void beforeClass() throws NoSuchAlgorithmException, SQLException {
		try (Connection conn = DriverManager.getConnection(BASE_URL, USER, PASSWORD)) {
			while (true) {
				testDBName = "transaction_manager_test_" + RandomStringUtils.randomAlphabetic(8);
				try {
					try (PreparedStatement preparedStatement = conn.prepareStatement("CREATE DATABASE " + testDBName)) {
						preparedStatement.executeUpdate();
					}
					break;
				} catch (SQLException e) {
					// re-do
				}
			}
		}
	}

	@AfterClass
	@SuppressFBWarnings
	public static void afterClass() throws SQLException {
		try (Connection conn = DriverManager.getConnection(BASE_URL, USER, PASSWORD)) {
			try (PreparedStatement preparedStatement = conn.prepareStatement("DROP DATABASE " + testDBName)) {
				preparedStatement.executeUpdate();
			}
		}
	}

	@Before
	@SuppressFBWarnings
	public void before() throws SQLException {
		try (Connection conn = DriverManager.getConnection(BASE_URL, USER, PASSWORD)) {
			try (PreparedStatement preparedStatement = conn.prepareStatement("DROP DATABASE " + testDBName)) {
				preparedStatement.executeUpdate();
			}
			try (PreparedStatement preparedStatement = conn.prepareStatement("CREATE DATABASE " + testDBName)) {
				preparedStatement.executeUpdate();
			}
		}

		String url = BASE_URL + "/" + testDBName;
		connection = DriverManager.getConnection(url, USER, PASSWORD);
		try (PreparedStatement preparedStatement = connection.prepareStatement("CREATE TABLE foo ("
			+ "id INT UNSIGNED NOT NULL AUTO_INCREMENT,"
			+ "var VARCHAR(32) NOT NULL,"
			+ "PRIMARY KEY (id)"
			+ ")")) {
			preparedStatement.executeUpdate();
		}
	}

	@After
	public void after() throws SQLException {
		connection.close();
	}
}
