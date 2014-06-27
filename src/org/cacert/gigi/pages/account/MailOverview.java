package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.output.MailTable;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

public class MailOverview extends Page {
	public static final String DEFAULT_PATH = "/account/mails";
	private MailTable table = new MailTable("mails", "userMail");
	public MailOverview(String title) {
		super(title);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		HashMap<String, Object> vars = new HashMap<String, Object>();
		User us = LoginPage.getUser(req);
		int id = us.getId();
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
					"SELECT * from `email` WHERE `memid`=? AND `deleted`=0");
			ps.setInt(1, id);
			ResultSet rs = ps.executeQuery();
			vars.put("mails", rs);
			vars.put("userMail", us.getEmail());

		} catch (SQLException e) {
			e.printStackTrace();
		}
		Language language = getLanguage(req);
		table.output(resp.getWriter(), language, vars);
		PrintWriter wri = resp.getWriter();
		wri.println("<p>");
		wri.println(language
				.getTranslation("Please Note: You can not set an unverified account as a default account, and you can not remove a default account. To remove the default account you must set another verified account as the default."));
		wri.println("</p>");
	}

}
