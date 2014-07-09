package org.cacert.gigi.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;

public class Notary {
	public static void writeUserAgreement(int memid, String document, String method, String comment, boolean active,
		int secmemid) throws SQLException {
		PreparedStatement q = DatabaseConnection.getInstance().prepare(
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

	public static AssuranceResult checkAssuranceIsPossible(User assurer, User target) {
		if (assurer.getId() == target.getId()) {
			return AssuranceResult.CANNOT_ASSURE_SELF;
		}
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"SELECT 1 FROM `notary` where `to`=? and `from`=? AND `deleted`=0");
			ps.setInt(1, target.getId());
			ps.setInt(2, assurer.getId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				rs.close();
				return AssuranceResult.ALREADY_ASSUREED;
			}
			rs.close();
			if (!assurer.canAssure()) {
				return AssuranceResult.CANNOT_ASSURE;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return AssuranceResult.ASSURANCE_SUCCEDED;
	}

	public enum AssuranceResult {
		CANNOT_ASSURE("You cannot assure."), ALREADY_ASSUREED("You already assured this person."), CANNOT_ASSURE_SELF(
			"Cannot assure myself."), ASSURANCE_SUCCEDED(""), ASSUREE_CHANGED(
			"Person details changed. Please start over again."), POINTS_OUT_OF_RANGE("Points out of range.");
		private final String message;

		private AssuranceResult(String message) {
			this.message = message;
		}

		public String getMessage() {
			return message;
		}
	}

	public synchronized static AssuranceResult assure(User assurer, User target, int awarded, String location,
		String date) throws SQLException {
		AssuranceResult can = checkAssuranceIsPossible(assurer, target);
		if (can != AssuranceResult.ASSURANCE_SUCCEDED) {
			return can;
		}
		User u = new User(target.getId());
		if (!u.equals(target)) {
			return AssuranceResult.ASSUREE_CHANGED;
		}
		if (awarded > assurer.getMaxAssurePoints() || awarded < 0) {
			return AssuranceResult.POINTS_OUT_OF_RANGE;
		}

		PreparedStatement ps = DatabaseConnection.getInstance().prepare(
			"INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?");
		ps.setInt(1, assurer.getId());
		ps.setInt(2, target.getId());
		ps.setInt(3, awarded);
		ps.setString(4, location);
		ps.setString(5, date);
		ps.execute();
		return AssuranceResult.ASSURANCE_SUCCEDED;
	}
}
