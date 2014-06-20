package org.cacert.gigi;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.MainPage;

public class Gigi extends HttpServlet {
	private HashMap<String, Page> pages = new HashMap<String, Page>();

	@Override
	public void init() throws ServletException {
		pages.put("/login", new LoginPage());
		pages.put("/", new MainPage());
		super.init();
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		X509Certificate[] cert = (X509Certificate[]) req
				.getAttribute("javax.servlet.request.X509Certificate");
		HttpSession hs = req.getSession(false);
		if (hs == null || !((Boolean) hs.getAttribute("loggedin"))) {
			if (cert != null) {
				tryAuthWithCertificate(req, cert[0]);
				hs = req.getSession(false);
			}
		}
		if (hs != null && ((Boolean) hs.getAttribute("loggedin"))
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
				hs.setAttribute("loggedin", false);
				hs.invalidate();
			}
			resp.sendRedirect("/");
			return;
		}

		if ((hs == null || !((Boolean) hs.getAttribute("loggedin")))
				&& !"/login".equals(req.getPathInfo())) {
			System.out.println(req.getPathInfo());
			resp.sendRedirect("/login");
			return;
		}
		if (pages.containsKey(req.getPathInfo())) {
			Page p = pages.get(req.getPathInfo());
			p.doGet(req, resp);
		} else {
			resp.sendError(404, "Page not found.");
		}

	}

	private void authWithUnpw(HttpServletRequest req) {
		String un = req.getParameter("username");
		String pw = req.getParameter("password");
		// TODO dummy password check if (un.equals(pw)) {
		HttpSession hs = req.getSession();
		hs.setAttribute("loggedin", true);
	}

	private void tryAuthWithCertificate(HttpServletRequest req,
			X509Certificate x509Certificate) {
		// TODO ckeck if certificate is valid
		HttpSession hs = req.getSession();
		hs.setAttribute("loggedin", true);
	}
}
