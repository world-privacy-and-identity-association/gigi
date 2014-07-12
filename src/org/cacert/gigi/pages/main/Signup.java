package org.cacert.gigi.pages.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.HTMLEncoder;
import org.cacert.gigi.util.Notary;
import org.cacert.gigi.util.PasswordStrengthChecker;
import org.cacert.gigi.util.RandomToken;
import org.cacert.gigi.util.ServerConstants;

public class Signup extends Form {
	User buildup = new User();
	Template t;
	boolean general = true, country = true, regional = true, radius = true;

	public Signup(HttpServletRequest hsr) {
		super(hsr);
		t = new Template(Signup.class.getResource("Signup.templ"));
		buildup.setFname("");
		buildup.setMname("");
		buildup.setLname("");
		buildup.setSuffix("");
		buildup.setEmail("");
		buildup.setDob(new Date(0));
	}

	DateSelector myDoB = new DateSelector("day", "month", "year");

	@Override
	public void outputContent(PrintWriter out, Language l, Map<String, Object> outerVars) {
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
		vars.put("helpOnNames", String.format(l.getTranslation("Help on Names %sin the wiki%s"),
			"<a href=\"//wiki.cacert.org/FAQ/HowToEnterNamesInJoinForm\" target=\"_blank\">", "</a>"));
		vars.put("csrf", getCSRFToken());
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

	@Override
	public synchronized boolean submit(PrintWriter out, HttpServletRequest req) {
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
			outputError(out, req, "You have to agree to the CAcert Community agreement.");
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
		int pwpoints = PasswordStrengthChecker.checkpw(pw1, buildup);
		if (pwpoints < 3) {
			outputError(out, req, "The Pass Phrase you submitted failed to contain enough"
				+ " differing characters and/or contained words from" + " your name and/or email address.");
			failed = true;
		}
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
				outputError(out, req, "This email address is currently valid in the system.");
				failed = true;
			}
			r1.close();
			r2.close();
			PreparedStatement q3 = DatabaseConnection.getInstance().prepare(
				"select `domain` from `baddomains` where `domain`=RIGHT(?, LENGTH(`domain`))");
			q3.setString(1, buildup.getEmail());

			ResultSet r3 = q3.executeQuery();
			if (r3.next()) {
				String domain = r3.getString(1);
				out.print("<div>");
				out.print(String.format(
					Page.translate(req, "We don't allow signups from people using email addresses from %s"), domain));
				out.println("</div>");
				failed = true;
			}
			r3.close();
		} catch (SQLException e) {
			e.printStackTrace();
			failed = true;
		}
		String mailResult = EmailProvider.FAIL;
		try {
			mailResult = EmailProvider.getInstance().checkEmailServer(0, buildup.getEmail());
		} catch (IOException e) {
		}
		if (!mailResult.equals(EmailProvider.OK)) {
			if (mailResult.startsWith("4")) {
				outputError(out, req, "The mail server responsible for your domain indicated"
					+ " a temporary failure. This may be due to anti-SPAM measures, such"
					+ " as greylisting. Please try again in a few minutes.");
			} else {
				outputError(out, req, "Email Address given was invalid, or a test connection"
					+ " couldn't be made to your server, or the server" + " rejected the email address as invalid");
			}
			if (mailResult.equals(EmailProvider.FAIL)) {
				outputError(out, req, "Failed to make a connection to the mail server");
			} else {
				out.print("<div>");
				out.print(mailResult);
				out.println("</div>");
			}
			failed = true;
		}

		out.println("</div>");
		if (failed) {
			return false;
		}
		try {
			run(req, pw1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	private void run(HttpServletRequest req, String password) throws SQLException {
		try {
			DatabaseConnection.getInstance().beginTransaction();
			String hash = RandomToken.generateToken(16);

			buildup.setDob(myDoB.getDate());
			buildup.insert(password);
			int memid = buildup.getId();
			PreparedStatement ps = DatabaseConnection.getInstance().prepare(
				"insert into `email` set `email`=?," + " `hash`=?, `created`=NOW(),`memid`=?");
			ps.setString(1, buildup.getEmail());
			ps.setString(2, hash);
			ps.setInt(3, memid);
			ps.execute();
			int emailid = DatabaseConnection.lastInsertId(ps);
			ps = DatabaseConnection.getInstance().prepare(
				"insert into `alerts` set `memid`=?," + " `general`=?, `country`=?, `regional`=?, `radius`=?");
			ps.setInt(1, memid);
			ps.setString(2, general ? "1" : "0");
			ps.setString(3, country ? "1" : "0");
			ps.setString(4, regional ? "1" : "0");
			ps.setString(5, radius ? "1" : "0");
			ps.execute();
			Notary.writeUserAgreement(memid, "CCA", "account creation", "", true, 0);

			StringBuffer body = new StringBuffer();
			body.append(Page
				.translate(
					req,
					"Thanks for signing up with CAcert.org, below is the link you need to open to verify your account. Once your account is verified you will be able to start issuing certificates till your hearts' content!"));
			body.append("\n\nhttps://");
			body.append(ServerConstants.getWwwHostNamePort());
			body.append("/verify?type=email&id=");
			body.append(emailid);
			body.append("&hash=");
			body.append(hash);
			body.append("\n\n");
			body.append(Page.translate(req, "Best regards"));
			body.append("\n");
			body.append(Page.translate(req, "CAcert.org Support!"));
			try {
				EmailProvider.getInstance().sendmail(buildup.getEmail(),
					"[CAcert.org] " + Page.translate(req, "Mail Probe"), body.toString(), "support@cacert.org", null,
					null, null, null, false);
			} catch (IOException e) {
				e.printStackTrace();
			}
			DatabaseConnection.getInstance().commitTransaction();
		} finally {
			DatabaseConnection.getInstance().quitTransaction();
		}

	}
}
