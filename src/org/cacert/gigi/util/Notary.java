package org.cacert.gigi.util;

import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;

public class Notary {
	public static void writeUserAgreement(int memid, String document,
			String method, String comment, boolean active, int secmemid)
			throws SQLException {
		PreparedStatement q = DatabaseConnection
				.getInstance()
				.prepare(
						"insert into `user_agreements` set `memid`=?, `secmemid`=?,"
								+ " `document`=?,`date`=NOW(), `active`=?,`method`=?,`comment`=?");
		q.setInt(1, memid);
		q.setInt(2, secmemid);
		q.setString(3, document);
		q.setInt(4, active ? 1 : 0);
		q.setString(5, method);
		q.setString(6, comment);
		q.execute();
	}

	public static boolean checkAssuranceIsPossible(User assurer, User target,
			PrintWriter errOut) {
		if (assurer.getId() == target.getId()) {
			if (errOut != null) {
				errOut.println("Cannot assure myself.");
			}
			return false;
		}
		try {
			PreparedStatement ps = DatabaseConnection
					.getInstance()
					.prepare(
							"SELECT 1 FROM `notary` where `to`=? and `from`=? AND `deleted`=0");
			ps.setInt(1, target.getId());
			ps.setInt(2, assurer.getId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				if (errOut != null) {
					errOut.println("You already assured this person.");
				}
				rs.close();
				return false;
			}
			rs.close();
			if (!assurer.canAssure()) {
				if (errOut != null) {
					errOut.println("You cannot assure.");
				}
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	public synchronized static boolean assure(User assurer, User target,
			int awarded, String location, String date) throws SQLException {
		if (!checkAssuranceIsPossible(assurer, target, null)) {
			return false;
		}
		User u = new User(target.getId());
		if (!u.equals(target)) {
			return false;
		}
		System.out.println("Would now assure.");
		if (awarded > assurer.getMaxAssurePoints() || awarded < 0) {
			return false;
		}

		PreparedStatement ps = DatabaseConnection
				.getInstance()
				.prepare(
						"INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?");
		ps.setInt(1, assurer.getId());
		ps.setInt(2, target.getId());
		ps.setInt(3, awarded);
		ps.setString(4, location);
		ps.setString(5, date);
		ps.execute();
		return true;
	}
}
