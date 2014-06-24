package org.cacert.gigi.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;
import java.sql.Statement;

public class DatabaseConnection {
	public static final int CONNECTION_TIMEOUT = 24 * 60 * 60;
	Connection c;
	HashMap<String, PreparedStatement> statements = new HashMap<String, PreparedStatement>();
	private static Properties credentials;
	Statement adHoc;
	public DatabaseConnection() {
		try {
			Class.forName(credentials.getProperty("sql.driver"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		tryConnect();

	}
	private void tryConnect() {
		try {
			c = DriverManager.getConnection(credentials.getProperty("sql.url")
					+ "?zeroDateTimeBehavior=convertToNull",
					credentials.getProperty("sql.user"),
					credentials.getProperty("sql.password"));
			PreparedStatement ps = c
					.prepareStatement("SET SESSION wait_timeout=?;");
			ps.setInt(1, CONNECTION_TIMEOUT);
			ps.execute();
			ps.close();
			adHoc = c.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public PreparedStatement prepare(String query) throws SQLException {
		ensureOpen();
		PreparedStatement statement = statements.get(query);
		if (statement == null) {
			statement = c.prepareStatement(query);
			statements.put(query, statement);
		}
		return statement;
	}
	long lastAction = System.currentTimeMillis();
	private void ensureOpen() {
		if (System.currentTimeMillis() - lastAction > CONNECTION_TIMEOUT * 1000L) {
			try {
				ResultSet rs = adHoc.executeQuery("SELECT 1");
				rs.close();
				lastAction = System.currentTimeMillis();
				return;
			} catch (SQLException e) {
			}
			statements.clear();
			tryConnect();
		}
		lastAction = System.currentTimeMillis();
	}
	public static int lastInsertId(PreparedStatement query) throws SQLException {
		ResultSet rs = query.getGeneratedKeys();
		rs.next();
		int id = rs.getInt(1);
		rs.close();
		return id;
	}
	static ThreadLocal<DatabaseConnection> instances = new ThreadLocal<DatabaseConnection>() {
		@Override
		protected DatabaseConnection initialValue() {
			return new DatabaseConnection();
		}
	};
	public static DatabaseConnection getInstance() {
		return instances.get();
	}
	public static void init(Properties conf) {
		if (credentials != null) {
			throw new Error("Re-initiaizing is forbidden.");
		}
		credentials = conf;
	}
}
