package org.cacert.gigi;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class Gigi extends HttpServlet {
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

		if (hs == null || !((Boolean) hs.getAttribute("loggedin"))) {
			resp.setContentType("text/html");
			resp.getWriter().println("Access denied. Sending login form.");
			resp.getWriter()
					.println(
							"<form method='POST' action='/login'>"
									+ "<input type='text' name='username'>"
									+ "<input type='password' name='password'> <input type='submit' value='login'></form>");
			return;
		}
		resp.getWriter().println("Access granted.");

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
