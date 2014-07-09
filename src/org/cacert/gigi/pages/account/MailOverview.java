package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

public class MailOverview extends Page {
	public static final String DEFAULT_PATH = "/account/mails";

	public MailOverview(String title) {
		super(title);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		User us = LoginPage.getUser(req);
		int id = us.getId();
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
					"SELECT * from `email` WHERE `memid`=? AND `deleted`=0");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
