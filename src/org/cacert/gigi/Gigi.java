package org.cacert.gigi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.MainPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.main.RegisterPage;
import org.cacert.gigi.util.PasswordHash;
import org.eclipse.jetty.util.log.Log;

public class Gigi extends HttpServlet {
	public static final String LOGGEDIN = "loggedin";
	public static final String USER = "user";
	private static final long serialVersionUID = -6386785421902852904L;
	private String[] baseTemplate;
	private HashMap<String, Page> pages = new HashMap<String, Page>();

	@Override
	public void init() throws ServletException {
		pages.put("/login", new LoginPage("CACert - Login"));
		pages.put("/", new MainPage("CACert - Home"));
		pages.put(RegisterPage.PATH, new RegisterPage());
		String templ = "";
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File("templates/base.html"))));
			String tmp;
			while ((tmp = reader.readLine()) != null) {
				templ += tmp;
			}
			baseTemplate = templ.split("\\$content\\$");
		} catch (Exception e) {
			Log.getLogger(Gigi.class).warn("Error loading template!", e);
		}
		super.init();

	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		X509Certificate[] cert = (X509Certificate[]) req
				.getAttribute("javax.servlet.request.X509Certificate");
		HttpSession hs = req.getSession();
		if (hs.getAttribute(LOGGEDIN) == null) {
			if (cert != null) {
				tryAuthWithCertificate(req, cert[0]);
				hs = req.getSession();
			}
		}
		if (hs.getAttribute("loggedin") != null
				&& req.getPathInfo().equals("/login")) {
			resp.sendRedirect("/");
			return;
		}
		if (req.getMethod().equals("POST") && req.getPathInfo() != null
				&& req.getPathInfo().equals("/login")) {
			authWithUnpw(req);
			resp.sendRedirect("/");
			return;
		}
		if (req.getPathInfo() != null && req.getPathInfo().equals("/logout")) {
			if (hs != null) {
				hs.setAttribute(LOGGEDIN, null);
				hs.invalidate();
			}
			resp.sendRedirect("/");
			return;
		}

		if (hs.getAttribute("loggedin") == null
				&& !"/login".equals(req.getPathInfo())) {
			System.out.println(req.getPathInfo());
			resp.sendRedirect("/login");
			return;
		}
		if (pages.containsKey(req.getPathInfo())) {
			String b0 = baseTemplate[0];
			Page p = pages.get(req.getPathInfo());
			b0 = makeDynTempl(b0, p);
			resp.setContentType("text/html; charset=utf-8");
			resp.getWriter().print(b0);
			if (hs != null && hs.getAttribute(LOGGEDIN) != null) {
				resp.getWriter().println(
						"Hi " + ((User) hs.getAttribute(USER)).getFname());
			}
			if (req.getMethod().equals("POST")) {
				p.doPost(req, resp);
			} else {
				p.doGet(req, resp);
			}
			String b1 = baseTemplate[1];
			b1 = makeDynTempl(b1, p);
			resp.getWriter().print(b1);
		} else {
			resp.sendError(404, "Page not found.");
		}

	}
	private String makeDynTempl(String in, Page p) {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		in = in.replaceAll("\\$title\\$", p.getTitle());
		in = in.replaceAll("\\$year\\$", year + "");
		return in;
	}
	private void authWithUnpw(HttpServletRequest req) {
		String un = req.getParameter("username");
		String pw = req.getParameter("password");
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
					"SELECT `password`, `id` FROM `users` WHERE `email`=?");
			ps.setString(1, un);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				if (PasswordHash.verifyHash(pw, rs.getString(1))) {
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
