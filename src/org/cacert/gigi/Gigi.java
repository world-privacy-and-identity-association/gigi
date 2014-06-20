package org.cacert.gigi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.MainPage;
import org.cacert.gigi.pages.Page;
import org.eclipse.jetty.util.log.Log;

public class Gigi extends HttpServlet {
	private static final long serialVersionUID = -6386785421902852904L;
	private String[] baseTemplate;
	private HashMap<String, Page> pages = new HashMap<String, Page>();

	@Override
	public void init() throws ServletException {
		pages.put("/login", new LoginPage("CACert - Login"));
		pages.put("/", new MainPage("CACert - Home"));
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
			String b0 = baseTemplate[0];
			Page p = pages.get(req.getPathInfo());
			b0 = b0.replaceAll("\\$title\\$", p.getTitle());
			resp.getWriter().print(b0);
			p.doGet(req, resp);
			String b1 = baseTemplate[1];
			b1 = b1.replaceAll("\\$title\\$", p.getTitle());
			resp.getWriter().print(b1);
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
