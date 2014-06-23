package org.cacert.gigi.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

public class DatabaseConnection {
	Connection c;
	HashMap<String, PreparedStatement> statements = new HashMap<String, PreparedStatement>();
	static Properties credentials = new Properties();
	static {
		try {
			credentials.load(new FileInputStream("config/sql.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public DatabaseConnection() {
		try {
			Class.forName(credentials.getProperty("driver"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		try {
			c = DriverManager.getConnection(credentials.getProperty("url"),
					credentials.getProperty("user"),
					credentials.getProperty("password"));
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
	public PreparedStatement prepare(String query) throws SQLException {
		PreparedStatement statement = statements.get(query);
		if (statement == null) {
			statement = c.prepareStatement(query);
			statements.put(query, statement);
		}
		return statement;
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
}
