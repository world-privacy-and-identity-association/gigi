package org.cacert.gigi.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.cacert.gigi.database.DatabaseConnection;

public class Notary {
	public static void writeUserAgreement(int memid, String document,
			String method, String comment, boolean active, int secmemid) {
		try {
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
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
