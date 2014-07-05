package org.cacert.gigi.pages;

import static org.cacert.gigi.Gigi.LOGGEDIN;
import static org.cacert.gigi.Gigi.USER;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.util.PasswordHash;

public class LoginPage extends Page {
	public static final String LOGIN_RETURNPATH = "login-returnpath";

	public LoginPage(String title) {
		super(title);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.getWriter()
				.println(
						"<form method='POST' action='/login'>"
								+ "<input type='text' name='username'>"
								+ "<input type='password' name='password'> <input type='submit' value='login'></form>");
	}

	@Override
	public boolean beforeTemplate(HttpServletRequest req,
			HttpServletResponse resp) throws IOException {
		String redir = (String) req.getSession().getAttribute(LOGIN_RETURNPATH);
		if (req.getSession().getAttribute("loggedin") == null) {
			X509Certificate[] cert = (X509Certificate[]) req
					.getAttribute("javax.servlet.request.X509Certificate");
			if (cert != null && cert[0] != null) {
				tryAuthWithCertificate(req, cert[0]);
			}
			if (req.getMethod().equals("POST")) {
				tryAuthWithUnpw(req);
			}
		}

		if (req.getSession().getAttribute("loggedin") != null) {
			String s = redir;
			if (s != null) {
				if (!s.startsWith("/")) {
					s = "/" + s;
				}
				resp.sendRedirect(s);
			} else {
				resp.sendRedirect("/");
			}
			return true;
		}
		return false;
	}
	@Override
	public boolean needsLogin() {
		return false;
	}
	private void tryAuthWithUnpw(HttpServletRequest req) {
		String un = req.getParameter("username");
		String pw = req.getParameter("password");
		try {
			PreparedStatement ps = DatabaseConnection
					.getInstance()
					.prepare(
							"SELECT `password`, `id` FROM `users` WHERE `email`=? AND locked='0' AND verified='1'");
			ps.setString(1, un);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				if (PasswordHash.verifyHash(pw, rs.getString(1))) {
					req.getSession().invalidate();
					HttpSession hs = req.getSession();
					hs.setAttribute(LOGGEDIN, true);
					hs.setAttribute(USER, new User(rs.getInt(2)));
				}
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public static User getUser(HttpServletRequest req) {
		return (User) req.getSession().getAttribute(USER);
	}
	private void tryAuthWithCertificate(HttpServletRequest req,
			X509Certificate x509Certificate) {
		String serial = x509Certificate.getSerialNumber().toString(16)
				.toUpperCase();
		try {
			PreparedStatement ps = DatabaseConnection
					.getInstance()
					.prepare(
							"SELECT `memid` FROM `emailcerts` WHERE `serial`=? AND `disablelogin`='0' AND `revoked` = "
									+ "'0000-00-00 00:00:00'");
			ps.setString(1, serial);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				req.getSession().invalidate();
				HttpSession hs = req.getSession();
				hs.setAttribute(LOGGEDIN, true);
				hs.setAttribute(USER, new User(rs.getInt(1)));
			}
			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
