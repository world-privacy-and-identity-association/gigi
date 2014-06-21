package org.cacert.gigi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cacert.gigi.database.DatabaseConnection;

public class User {

	private int id;
	String fname;
	String lname;

	public User(int id) {
		this.id = id;
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
					"SELECT `fname`, `lname` FROM `users` WHERE id=?");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				fname = rs.getString(1);
				lname = rs.getString(2);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public int getId() {
		return id;
	}
	public String getFname() {
		return fname;
	}
	public String getLname() {
		return lname;
	}

}
