package net.moznion.transaction.manager;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class TestBase {
	private static final String baseUrl = "jdbc:mysql://localhost";
	private static final String user = "root";
	private static final String password = "";
	private static String testDBName;

	protected static Connection connection;

	@BeforeClass
	public static void beforeClass() throws NoSuchAlgorithmException, SQLException {
		try (Connection conn = DriverManager.getConnection(baseUrl, user, password)) {
			while (true) {
				testDBName = "transaction_manager_test_" + RandomStringUtils.randomAlphabetic(8);
				try {
					conn.prepareStatement("CREATE DATABASE " + testDBName).executeUpdate();
					break;
				} catch (SQLException e) {
					// re-do
				}
			}
		}

	}

	@AfterClass
	public static void afterClass() throws SQLException {
		try (Connection conn = DriverManager.getConnection(baseUrl, user, password)) {
			conn.prepareStatement("DROP DATABASE " + testDBName).executeUpdate();
		}
	}

	@Before
	public void before() throws SQLException {
		try (Connection conn = DriverManager.getConnection(baseUrl, user, password)) {
			conn.prepareStatement("DROP DATABASE " + testDBName).executeUpdate();
			conn.prepareStatement("CREATE DATABASE " + testDBName).executeUpdate();
		}

		String url = baseUrl + "/" + testDBName;
		connection = DriverManager.getConnection(url, user, password);
		connection.prepareStatement("CREATE TABLE foo ("
			+ "id INT UNSIGNED NOT NULL AUTO_INCREMENT,"
			+ "var VARCHAR(32) NOT NULL,"
			+ "PRIMARY KEY (id)"
			+ ")").executeUpdate();
	}

	@After
	public void after() throws SQLException {
		connection.close();
	}
}
