package org.cacert.gigi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.util.PasswordHash;

public class User {

	private int id;
	String fname;
	String mname;
	String lname;
	String suffix;
	Date dob;
	String email;

	public User(int id) {
		this.id = id;
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
					"SELECT `fname`, `lname`, `dob` FROM `users` WHERE id=?");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				fname = rs.getString(1);
				lname = rs.getString(2);
				dob = rs.getDate(3);
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public User() {
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
	public String getMname() {
		return mname;
	}
	public void setMname(String mname) {
		this.mname = mname;
	}
	public String getSuffix() {
		return suffix;
	}
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	public Date getDob() {
		return dob;
	}
	public void setDob(Date dob) {
		this.dob = dob;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public void setId(int id) {
		this.id = id;
	}
	public void setFname(String fname) {
		this.fname = fname;
	}
	public void setLname(String lname) {
		this.lname = lname;
	}
	public void insert(String password) throws SQLException {
		if (id != 0) {
			throw new Error("refusing to insert");
		}
		PreparedStatement query = DatabaseConnection.getInstance().prepare(
				"insert into `users` set `email`=?, `password`=?, "
						+ "`fname`=?, `mname`=?, `lname`=?, "
						+ "`suffix`=?, `dob`=?, `created`=NOW(), locked=0");
		query.setString(1, email);
		query.setString(2, PasswordHash.hash(password));
		query.setString(3, fname);
		query.setString(4, mname);
		query.setString(5, lname);
		query.setString(6, suffix);
		query.setDate(7, new java.sql.Date(dob.getTime()));
		query.execute();
		id = DatabaseConnection.lastInsertId(query);
		System.out.println("Inserted: " + id);
	}

}
