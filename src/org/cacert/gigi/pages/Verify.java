package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.database.DatabaseConnection;

public class Verify extends Page {
	public static final String PATH = "/verify";

	public Verify() {
		super("Verify email");
	}

	@Override
	public boolean needsLogin() {
		return false;
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter out = resp.getWriter();
		String hash = req.getParameter("hash");
		String type = req.getParameter("type");
		String id = req.getParameter("id");
		if ("email".equals(type)) {
			try {
				PreparedStatement ps = DatabaseConnection.getInstance().prepare(
					"select email, memid from `email` where `id`=? and `hash`=? and `hash` != '' and `deleted` = 0");
				ps.setString(1, id);
				ps.setString(2, hash);
				ResultSet rs = ps.executeQuery();
				rs.last();
				if (rs.getRow() == 1) {
					PreparedStatement ps1 = DatabaseConnection.getInstance().prepare(
						"update `email` set `hash`='', `modified`=NOW() where `id`=?");
					ps1.setString(1, id);
					ps1.execute();
					PreparedStatement ps2 = DatabaseConnection.getInstance().prepare(
						"update `users` set `verified`='1' where `id`=? and `email`=? and `verified`='0'");
					ps2.setString(1, rs.getString(2));
					ps2.setString(2, rs.getString(1));
					ps2.execute();
					out.println("Your email is good.");
				} else {
					out.println("Your request is invalid");
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String hash = req.getParameter("hash");
		String type = req.getParameter("type");
		if ("email".equals(type)) {

		}
	}
}
