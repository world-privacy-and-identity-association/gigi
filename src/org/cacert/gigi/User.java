package org.cacert.gigi;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.util.PasswordHash;
import org.cacert.gigi.util.PasswordStrengthChecker;

public class User {

	private int id;
	Name name = new Name(null, null, null, null);

	Date dob;
	String email;

	public User(int id) {
		this.id = id;
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"SELECT `fname`, `lname`,`mname`, `suffix`, `dob`, `email` FROM `users` WHERE id=?");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				name = new Name(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4));
				dob = rs.getDate(5);
				email = rs.getString(6);
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
			"insert into `users` set `email`=?, `password`=?, " + "`fname`=?, `mname`=?, `lname`=?, "
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
	}

	public void changePassword(String oldPass, String newPass) throws GigiApiException {
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare("SELECT `password` FROM users WHERE id=?");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			if (!rs.next()) {
				throw new GigiApiException("User not found... very bad.");
			}
			if (!PasswordHash.verifyHash(oldPass, rs.getString(1))) {
				throw new GigiApiException("Old password does not match.");
			}
			rs.close();
			PasswordStrengthChecker.assertStrongPassword(newPass, this);
			ps = DatabaseConnection.getInstance().prepare("UPDATE users SET `password`=? WHERE id=?");
			ps.setString(1, PasswordHash.hash(newPass));
			ps.setInt(2, id);
			if (ps.executeUpdate() != 1) {
				throw new GigiApiException("Password update failed.");
			}
		} catch (SQLException e) {
			throw new GigiApiException(e);
		}
	}

	public boolean canAssure() throws SQLException {
		if (getAssurancePoints() < 100) {
			return false;
		}

		return hasPassedCATS();

	}

	public boolean hasPassedCATS() throws SQLException {
		PreparedStatement query = DatabaseConnection.getInstance().prepare(
			"SELECT 1 FROM `cats_passed` where `user_id`=?");
		query.setInt(1, id);
		ResultSet rs = query.executeQuery();
		if (rs.next()) {
			return true;
		} else {
			return false;
		}
	}

	public int getAssurancePoints() throws SQLException {
		PreparedStatement query = DatabaseConnection.getInstance().prepare(
			"SELECT sum(points) FROM `notary` where `to`=? AND `deleted`=0");
		query.setInt(1, id);
		ResultSet rs = query.executeQuery();
		int points = 0;
		if (rs.next()) {
			points = rs.getInt(1);
		}
		rs.close();
		return points;
	}

	public int getExperiencePoints() throws SQLException {
		PreparedStatement query = DatabaseConnection.getInstance().prepare(
			"SELECT count(*) FROM `notary` where `from`=? AND `deleted`=0");
		query.setInt(1, id);
		ResultSet rs = query.executeQuery();
		int points = 0;
		if (rs.next()) {
			points = rs.getInt(1) * 2;
		}
		rs.close();
		return points;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof User)) {
			return false;
		}
		User s = (User) obj;
		return name.equals(s.name) && email.equals(s.email) && dob.toString().equals(s.dob.toString()); // This
																										// is
																										// due
																										// to
																										// day
																										// cutoff
	}

	/**
	 * Gets the maximum allowed points NOW. Note that an assurance needs to
	 * re-check PoJam as it has taken place in the past.
	 * 
	 * @return the maximal points
	 * @throws SQLException
	 */
	public int getMaxAssurePoints() throws SQLException {
		int exp = getExperiencePoints();
		int points = 10;
		Calendar c = Calendar.getInstance();
		c.setTime(dob);
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH);
		int day = c.get(Calendar.DAY_OF_MONTH);
		c.set(year + 18, month, day);
		if (System.currentTimeMillis() < c.getTime().getTime()) {
			return points; // not 18 Years old.
		}

		if (exp >= 10) {
			points += 5;
		}
		if (exp >= 20) {
			points += 5;
		}
		if (exp >= 30) {
			points += 5;
		}
		if (exp >= 40) {
			points += 5;
		}
		if (exp >= 50) {
			points += 5;
		}
		return points;
	}

	public static User getById(int id) {
		return new User(id);
	}

	public EmailAddress[] getEmails() {
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"SELECT id FROM email WHERE memid=? AND deleted=0");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			rs.last();
			int count = rs.getRow();
			EmailAddress[] data = new EmailAddress[count];
			rs.beforeFirst();
			for (int i = 0; i < data.length; i++) {
				if (!rs.next()) {
					throw new Error("Internal sql api violation.");
				}
				data[i] = EmailAddress.getById(rs.getInt(1));
			}
			rs.close();
			return data;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	public Domain[] getDomains() {
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"SELECT id FROM domain WHERE memid=? AND deleted IS NULL");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			rs.last();
			int count = rs.getRow();
			Domain[] data = new Domain[count];
			rs.beforeFirst();
			for (int i = 0; i < data.length; i++) {
				if (!rs.next()) {
					throw new Error("Internal sql api violation.");
				}
				data[i] = Domain.getById(rs.getInt(1));
			}
			rs.close();
			return data;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void updateDefaultEmail(EmailAddress newMail) throws GigiApiException {
		try {
			EmailAddress[] adrs = getEmails();
			for (int i = 0; i < adrs.length; i++) {
				if (adrs[i].getAddress().equals(newMail.getAddress())) {
					if (!adrs[i].isVerified()) {
						throw new GigiApiException("Email not verified.");
					}
					PreparedStatement ps = DatabaseConnection.getInstance().prepare(
						"UPDATE users SET email=? WHERE id=?");
					ps.setString(1, newMail.getAddress());
					ps.setInt(2, getId());
					ps.execute();
					email = newMail.getAddress();
					return;
				}
			}
			throw new GigiApiException("Given address not an address of the user.");
		} catch (SQLException e) {
			throw new GigiApiException(e);
		}
	}

	public void deleteEmail(EmailAddress mail) throws GigiApiException {
		if (getEmail().equals(mail.getAddress())) {
			throw new GigiApiException("Can't delete user's default e-mail.");
		}
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare("UPDATE email SET deleted=? WHERE id=?");
			ps.setDate(1, new Date(System.currentTimeMillis()));
			ps.setInt(2, mail.getId());
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new GigiApiException(e);
		}
	}
}
