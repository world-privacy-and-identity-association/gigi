package org.cacert.gigi;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.output.Outputable;
import org.cacert.gigi.output.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.MainPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.TestSecure;
import org.cacert.gigi.pages.Verify;
import org.cacert.gigi.pages.account.MailAdd;
import org.cacert.gigi.pages.account.MailCertificates;
import org.cacert.gigi.pages.account.MailOverview;
import org.cacert.gigi.pages.account.MyDetails;
import org.cacert.gigi.pages.main.RegisterPage;
import org.cacert.gigi.pages.wot.AssurePage;
import org.cacert.gigi.util.ServerConstants;

public class Gigi extends HttpServlet {
	public static final String LOGGEDIN = "loggedin";
	public static final String USER = "user";
	private static final long serialVersionUID = -6386785421902852904L;
	private Template baseTemplate;
	private HashMap<String, Page> pages = new HashMap<String, Page>();

	public Gigi(Properties conf) {
		EmailProvider.init(conf);
		DatabaseConnection.init(conf);
	}
	@Override
	public void init() throws ServletException {
		pages.put("/login", new LoginPage("CACert - Login"));
		pages.put("/", new MainPage("CACert - Home"));
		pages.put("/secure", new TestSecure());
		pages.put(Verify.PATH, new Verify());
		pages.put(AssurePage.PATH + "/*", new AssurePage());
		pages.put(MailCertificates.PATH, new MailCertificates());
		pages.put(MyDetails.PATH, new MyDetails());
		pages.put(RegisterPage.PATH, new RegisterPage());
		pages.put(MailOverview.DEFAULT_PATH, new MailOverview(
				"My email addresses"));
		pages.put(MailAdd.DEFAULT_PATH, new MailAdd("Add new email"));
		baseTemplate = new Template(new InputStreamReader(
				Gigi.class.getResourceAsStream("Gigi.templ")));
		super.init();

	}
	@Override
	protected void service(final HttpServletRequest req,
			final HttpServletResponse resp) throws ServletException,
			IOException {
		addXSSHeaders(resp);
		if (req.getHeader("Origin") != null) {
			resp.getWriter().println("No cross domain access allowed.");
			return;
		}
		HttpSession hs = req.getSession();
		if (req.getPathInfo() != null && req.getPathInfo().equals("/logout")) {
			if (hs != null) {
				hs.setAttribute(LOGGEDIN, null);
				hs.invalidate();
			}
			resp.sendRedirect("/");
			return;
		}

		final Page p = getPage(req.getPathInfo());
		if (p != null) {

			if (p.needsLogin() && hs.getAttribute("loggedin") == null) {
				String request = req.getPathInfo();
				request = request.split("\\?")[0];
				hs.setAttribute(LoginPage.LOGIN_RETURNPATH, request);
				resp.sendRedirect("/login");
				return;
			}
			if (p.beforeTemplate(req, resp)) {
				return;
			}
			HashMap<String, Object> vars = new HashMap<String, Object>();

			resp.setContentType("text/html; charset=utf-8");
			Outputable content = new Outputable() {

				@Override
				public void output(PrintWriter out, Language l,
						Map<String, Object> vars) {
					try {
						if (req.getMethod().equals("POST")) {
							p.doPost(req, resp);
						} else {
							p.doGet(req, resp);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			};
			vars.put("title", p.getTitle());
			vars.put("static", ServerConstants.getStaticHostNamePort());
			vars.put("year", Calendar.getInstance().get(Calendar.YEAR));
			vars.put("content", content);
			baseTemplate.output(resp.getWriter(), Page.getLanguage(req), vars);
		} else {
			resp.sendError(404, "Page not found.");
		}

	}
	private Page getPage(String pathInfo) {
		if (pathInfo.endsWith("/") && !pathInfo.equals("/")) {
			pathInfo = pathInfo.substring(0, pathInfo.length() - 1);
		}
		Page page = pages.get(pathInfo);
		if (page != null) {
			return page;
		}
		page = pages.get(pathInfo + "/*");
		if (page != null) {
			return page;
		}
		int idx = pathInfo.lastIndexOf('/');
		pathInfo = pathInfo.substring(0, idx);

		page = pages.get(pathInfo + "/*");
		if (page != null) {
			return page;
		}
		return null;

	}

	public static void addXSSHeaders(HttpServletResponse hsr) {
		hsr.addHeader("Access-Control-Allow-Origin", "https://"
				+ ServerConstants.getWwwHostNamePort() + " https://"
				+ ServerConstants.getSecureHostNamePort());
		hsr.addHeader("Access-Control-Max-Age", "60");

		hsr.addHeader("Content-Security-Policy", getDefaultCSP());

	}
	private static String defaultCSP = null;
	private static String getDefaultCSP() {
		if (defaultCSP == null) {
			StringBuffer csp = new StringBuffer();
			csp.append("default-src 'none';");
			csp.append("font-src https://"
					+ ServerConstants.getStaticHostNamePort());
			csp.append(";img-src https://"
					+ ServerConstants.getStaticHostNamePort());
			csp.append(";media-src 'none'; object-src 'none';");
			csp.append("script-src https://"
					+ ServerConstants.getStaticHostNamePort());
			csp.append(";style-src https://"
					+ ServerConstants.getStaticHostNamePort());
			csp.append(";form-action https://"
					+ ServerConstants.getSecureHostNamePort() + " https://"
					+ ServerConstants.getWwwHostNamePort());
			csp.append("report-url https://api.cacert.org/security/csp/report");
			defaultCSP = csp.toString();
		}
		return defaultCSP;
	}
}
