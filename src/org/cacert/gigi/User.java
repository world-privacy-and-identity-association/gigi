package org.cacert.gigi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.util.PasswordHash;

public class User {

	private int id;
	Name name;

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
				name = new Name(rs.getString(1), rs.getString(2));
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
		return name.fname;
	}
	public String getLname() {
		return name.lname;
	}
	public String getMname() {
		return name.mname;
	}
	public Name getName() {
		return name;
	}
	public void setMname(String mname) {
		this.name.mname = mname;
	}
	public String getSuffix() {
		return name.suffix;
	}
	public void setSuffix(String suffix) {
		this.name.suffix = suffix;
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
		this.name.fname = fname;
	}
	public void setLname(String lname) {
		this.name.lname = lname;
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
		query.setString(3, name.fname);
		query.setString(4, name.mname);
		query.setString(5, name.lname);
		query.setString(6, name.suffix);
		query.setDate(7, new java.sql.Date(dob.getTime()));
		query.execute();
		id = DatabaseConnection.lastInsertId(query);
		System.out.println("Inserted: " + id);
	}

}
