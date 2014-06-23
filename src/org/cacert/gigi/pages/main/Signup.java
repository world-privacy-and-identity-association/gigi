package org.cacert.gigi.pages.main;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.HTMLEncoder;

public class Signup {
	User buildup = new User();
	String password;
	String password2;
	Template t;
	boolean general = true, country = true, regional = true, radius = true;
	public Signup() {
		try {
			t = new Template(new InputStreamReader(
					Signup.class.getResourceAsStream("Signup.templ"), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		buildup.setFname("");
		buildup.setMname("");
		buildup.setLname("");
		buildup.setSuffix("");
		buildup.setEmail("");
		buildup.setDob(new Date(0));
	}
	DateSelector myDoB = new DateSelector("day", "month", "year");

	public void writeForm(PrintWriter out, Language l) {
		HashMap<String, Object> vars = new HashMap<String, Object>();
		vars.put("fname", HTMLEncoder.encodeHTML(buildup.getFname()));
		vars.put("mname", HTMLEncoder.encodeHTML(buildup.getMname()));
		vars.put("lname", HTMLEncoder.encodeHTML(buildup.getLname()));
		vars.put("suffix", HTMLEncoder.encodeHTML(buildup.getSuffix()));
		vars.put("dob", myDoB);
		vars.put("email", HTMLEncoder.encodeHTML(buildup.getEmail()));
		vars.put("general", general ? " checked=\"checked\"" : "");
		vars.put("country", country ? " checked=\"checked\"" : "");
		vars.put("regional", regional ? " checked=\"checked\"" : "");
		vars.put("radius", radius ? " checked=\"checked\"" : "");
		vars.put(
				"helpOnNames",
				String.format(
						l.getTranslation("Help on Names %sin the wiki%s"),
						"<a href=\"//wiki.cacert.org/FAQ/HowToEnterNamesInJoinForm\" target=\"_blank\">",
						"</a>"));
		t.output(out, l, vars);
	}
	private void update(HttpServletRequest r) {
		if (r.getParameter("fname") != null) {
			buildup.setFname(r.getParameter("fname"));
		}
		if (r.getParameter("lname") != null) {
			buildup.setLname(r.getParameter("lname"));
		}
		if (r.getParameter("mname") != null) {
			buildup.setMname(r.getParameter("mname"));
		}
		if (r.getParameter("suffix") != null) {
			buildup.setSuffix(r.getParameter("suffix"));
		}
		if (r.getParameter("email") != null) {
			buildup.setEmail(r.getParameter("email"));
		}
		general = "1".equals(r.getParameter("general"));
		country = "1".equals(r.getParameter("country"));
		regional = "1".equals(r.getParameter("regional"));
		radius = "1".equals(r.getParameter("radius"));
		myDoB.update(r);
	}

	public boolean submit(PrintWriter out, HttpServletRequest req) {
		update(req);
		boolean failed = false;
		out.println("<div class='formError'>");
		if (buildup.getFname().equals("") || buildup.getLname().equals("")) {
			outputError(out, req, "First and/or last names were blank.");
			failed = true;
		}
		if (!myDoB.isValid()) {
			outputError(out, req, "Invalid date of birth");
			failed = true;
		}
		if (!"1".equals(req.getParameter("cca_agree"))) {
			outputError(out, req,
					"You have to agree to the CAcert Community agreement.");
			failed = true;
		}
		if (buildup.getEmail().equals("")) {
			outputError(out, req, "Email Address was blank");
			failed = true;
		}
		String pw1 = req.getParameter("pword1");
		String pw2 = req.getParameter("pword2");
		if (pw1 == null || pw1.equals("")) {
			outputError(out, req, "Pass Phrases were blank");
			failed = true;
		} else if (!pw1.equals(pw2)) {
			outputError(out, req, "Pass Phrases don't match");
			failed = true;
		}
		// TODO check password strength
		if (failed) {
			out.println("</div>");
			return false;
		}
		try {
			PreparedStatement q1 = DatabaseConnection.getInstance().prepare(
					"select * from `email` where `email`=? and `deleted`=0");
			PreparedStatement q2 = DatabaseConnection.getInstance().prepare(
					"select * from `users` where `email`=? and `deleted`=0");
			q1.setString(1, buildup.getEmail());
			q2.setString(1, buildup.getEmail());
			ResultSet r1 = q1.executeQuery();
			ResultSet r2 = q2.executeQuery();
			if (r1.next() || r2.next()) {
				outputError(out, req,
						"This email address is currently valid in the system.");
				failed = true;
			}
			r1.close();
			r2.close();
			PreparedStatement q3 = DatabaseConnection
					.getInstance()
					.prepare(
							"select `domain` from `baddomains` where `domain`=RIGHT(?, LENGTH(`domain`))");
			q3.setString(1, buildup.getEmail());

			ResultSet r3 = q3.executeQuery();
			if (r3.next()) {
				String domain = r3.getString(1);
				out.print("<div>");
				out.print(String.format(
						Page.translate(req,
								"We don't allow signups from people using email addresses from %s"),
						domain));
				out.println("</div>");
				failed = true;
			}
			r3.close();
		} catch (SQLException e) {
			e.printStackTrace();
			failed = true;
		}
		// TODO fast-check mail

		out.println("</div>");
		if (failed) {
			return false;
		}
		// TODO start getting to work
		return true;
	}
	private void outputError(PrintWriter out, ServletRequest req, String text) {
		out.print("<div>");
		out.print(Page.translate(req, text));
		out.println("</div>");
	}
}
