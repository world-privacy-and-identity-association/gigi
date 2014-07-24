package org.cacert.gigi;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.util.RandomToken;
import org.cacert.gigi.util.ServerConstants;

public class EmailAddress {
	private String address;
	private int id;
	private User owner;
	private String hash = null;

	private EmailAddress(int id) throws SQLException {
		PreparedStatement ps = DatabaseConnection.getInstance().prepare(
			"SELECT memid, email, hash FROM `email` WHERE id=? AND deleted=0");
		ps.setInt(1, id);

		ResultSet rs = ps.executeQuery();
		if (!rs.next()) {
			throw new IllegalArgumentException("Invalid email id " + id);
		}
		this.id = id;
		owner = User.getById(rs.getInt(1));
		address = rs.getString(2);
		hash = rs.getString(3);
		rs.close();
	}

	public EmailAddress(String address, User owner) {
		if (!EmailProvider.MAIL.matcher(address).matches()) {
			throw new IllegalArgumentException("Invalid email.");
		}
		this.address = address;
		this.owner = owner;
		this.hash = RandomToken.generateToken(16);
	}

	public void insert(Language l) {
		if (id != 0) {
			throw new IllegalStateException("already inserted.");
		}
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"INSERT INTO `email` SET memid=?, hash=?, email=?");
			ps.setInt(1, owner.getId());
			ps.setString(2, hash);
			ps.setString(3, address);
			ps.execute();
			id = DatabaseConnection.lastInsertId(ps);
			StringBuffer body = new StringBuffer();
			body.append(l
				.getTranslation("Thanks for signing up with CAcert.org, below is the link you need to open to verify your account. Once your account is verified you will be able to start issuing certificates till your hearts' content!"));
			body.append("\n\nhttps://");
			body.append(ServerConstants.getWwwHostNamePort());
			body.append("/verify?type=email&id=");
			body.append(id);
			body.append("&hash=");
			body.append(hash);
			body.append("\n\n");
			body.append(l.getTranslation("Best regards"));
			body.append("\n");
			body.append(l.getTranslation("CAcert.org Support!"));
			EmailProvider.getInstance().sendmail(address, "[CAcert.org] " + l.getTranslation("Mail Probe"),
				body.toString(), "support@cacert.org", null, null, null, null, false);
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getId() {
		return id;
	}

	public String getAddress() {
		return address;
	}

	public synchronized void verify(String hash) throws GigiApiException {
		if (this.hash.equals(hash)) {

			try {
				PreparedStatement ps = DatabaseConnection.getInstance()
					.prepare("UPDATE `email` SET hash='' WHERE id=?");
				ps.setInt(1, id);
				ps.execute();
				hash = "";

				// Verify user with that primary email
				PreparedStatement ps2 = DatabaseConnection.getInstance().prepare(
					"update `users` set `verified`='1' where `id`=? and `email`=? and `verified`='0'");
				ps2.setInt(1, owner.getId());
				ps2.setString(2, address);
				ps2.execute();
				this.hash = "";
			} catch (SQLException e) {
				throw new GigiApiException(e);
			}

		} else {
			throw new GigiApiException("Email verification hash is invalid.");
		}
	}

	public static EmailAddress getById(int id) throws IllegalArgumentException {
		// TODO cache
		try {
			EmailAddress e = new EmailAddress(id);
			return e;
		} catch (SQLException e) {
			throw new IllegalArgumentException(e);
		}
	}

	public boolean isVerified() {
		return hash.isEmpty();
	}
}
