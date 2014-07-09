package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.Template;
import org.cacert.gigi.output.Form.CSRFError;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.Notary.AssuranceResult;

public class AssurePage extends Page {
	public static final String PATH = "/wot/assure";
	public static final String SESSION = "/wot/assure/FORM";
	DateSelector ds = new DateSelector("day", "month", "year");
	Template t;

	public AssurePage() {
		super("Assure someone");
		t = new Template(new InputStreamReader(AssuranceForm.class.getResourceAsStream("AssureeSearch.templ")));

	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		PrintWriter out = resp.getWriter();
		String pi = req.getPathInfo().substring(PATH.length());
		if (pi.length() > 1) {
			User myself = LoginPage.getUser(req);
			int mid = Integer.parseInt(pi.substring(1));
			AssuranceResult check = Notary.checkAssuranceIsPossible(myself, new User(mid));
			if (check != AssuranceResult.ASSURANCE_SUCCEDED) {
				out.println(translate(req, check.getMessage()));
				return;
			}
			HttpSession hs = req.getSession();
			AssuranceForm form = (AssuranceForm) hs.getAttribute(SESSION);
			if (form == null || form.assuree.getId() != mid) {
				form = new AssuranceForm(mid);
				hs.setAttribute(SESSION, form);
			}

			form.output(out, getLanguage(req), new HashMap<String, Object>());
			;
		} else {
			HashMap<String, Object> vars = new HashMap<String, Object>();
			vars.put("DoB", ds);
			t.output(out, getLanguage(req), vars);
		}
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		PrintWriter out = resp.getWriter();
		String pi = req.getPathInfo().substring(PATH.length());
		if (pi.length() > 1) {
			User myself = LoginPage.getUser(req);
			int mid = Integer.parseInt(pi.substring(1));
			if (mid == myself.getId()) {
				out.println("Cannot assure myself.");
				return;
			}

			AssuranceForm form = (AssuranceForm) req.getSession().getAttribute(SESSION);
			if (form == null) {
				out.println("No form found. This is an Error. Fill in the form again.");
				return;
			}
			try {
				form.submit(out, req);
			} catch (CSRFError e) {
				resp.sendError(500, "CSRF Failed");
				out.println(translate(req, "CSRF Token failed."));
			}

			return;
		}

		System.out.println("searching for");
		ResultSet rs = null;
		try {
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"SELECT id, verified FROM users WHERE email=? AND dob=? AND deleted=0");
			ps.setString(1, req.getParameter("email"));
			String day = req.getParameter("year") + "-" + req.getParameter("month") + "-" + req.getParameter("day");
			ps.setString(2, day);
			rs = ps.executeQuery();
			int id = 0;
			if (rs.next()) {
				id = rs.getInt(1);
				int verified = rs.getInt(2);
				if (rs.next()) {
					out.println("Error, ambigous user. Please contact support@cacert.org.");
				} else {
					if (verified == 0) {
						out.println(translate(req, "User is not yet verified. Please try again in 24 hours!"));
					}
					resp.sendRedirect(PATH + "/" + id);
				}
			} else {
				out.print("<div class='formError'>");

				out.println(translate(req, "I'm sorry, there was no email and date of birth matching"
					+ " what you entered in the system. Please double check" + " your information."));
				out.print("</div>");
			}

			rs.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
