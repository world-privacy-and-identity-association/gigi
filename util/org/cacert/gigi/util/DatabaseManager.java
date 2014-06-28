package org.cacert.gigi.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
	public static String readFile(File f) throws IOException {
		return new String(Files.readAllBytes(f.toPath()));
	}
	public static void main(String[] args) throws SQLException,
			ClassNotFoundException, IOException {
		if (args.length < 4) {
			System.err
					.println("Usage: com.mysql.jdbc.Driver jdbc:mysql://localhost/cacert user password");
			return;
		}
		run(args);
	}
	public static void run(String[] args) throws ClassNotFoundException,
			SQLException, IOException {
		Class.forName(args[0]);
		Connection conn = DriverManager
				.getConnection(args[1], args[2], args[3]);
		Statement stmt = conn.createStatement();
		addFile(stmt, new File("doc/tableStructure.sql"));
		File localData = new File("doc/sampleData.sql");
		if (localData.exists()) {
			addFile(stmt, localData);
		}
		stmt.executeBatch();
		stmt.close();
	}
	private static void addFile(Statement stmt, File f) throws IOException,
			SQLException {
		String sql = readFile(f);
		String[] stmts = sql.split(";");
		for (String string : stmts) {
			if (!string.trim().equals("")) {
				stmt.addBatch(string);
			}
		}
	}
}
