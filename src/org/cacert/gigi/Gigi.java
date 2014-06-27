package org.cacert.gigi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.MainPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.pages.PolicyRedir;
import org.cacert.gigi.pages.TestSecure;
import org.cacert.gigi.pages.Verify;
import org.cacert.gigi.pages.account.MailAdd;
import org.cacert.gigi.pages.account.MailCertificates;
import org.cacert.gigi.pages.account.MailOverview;
import org.cacert.gigi.pages.account.MyDetails;
import org.cacert.gigi.pages.main.RegisterPage;
import org.cacert.gigi.pages.wot.AssurePage;
import org.eclipse.jetty.util.log.Log;

public class Gigi extends HttpServlet {
	public static final String LOGGEDIN = "loggedin";
	public static final String USER = "user";
	private static final long serialVersionUID = -6386785421902852904L;
	private String[] baseTemplate;
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
		pages.put(AssurePage.PATH, new AssurePage());
		pages.put(MailCertificates.PATH, new MailCertificates());
		pages.put(MyDetails.PATH, new MyDetails());
		pages.put(RegisterPage.PATH, new RegisterPage());
		pages.put(PolicyRedir.PATH, new PolicyRedir());
		pages.put(MailOverview.DEFAULT_PATH, new MailOverview(
				"My email addresses"));
		pages.put(MailAdd.DEFAULT_PATH, new MailAdd("Add new email"));
		String templ = "";
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File("templates/base.html"))))) {
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
		HttpSession hs = req.getSession();
		if (req.getPathInfo() != null && req.getPathInfo().equals("/logout")) {
			if (hs != null) {
				hs.setAttribute(LOGGEDIN, null);
				hs.invalidate();
			}
			resp.sendRedirect("/");
			return;
		}

		Page p = getPage(req.getPathInfo());
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

			String b0 = baseTemplate[0];
			b0 = makeDynTempl(b0, p);
			resp.setContentType("text/html; charset=utf-8");
			resp.getWriter().print(b0);
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
	private String makeDynTempl(String in, Page p) {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		in = in.replaceAll("\\$title\\$", p.getTitle());
		in = in.replaceAll("\\$year\\$", year + "");
		return in;
	}

}
