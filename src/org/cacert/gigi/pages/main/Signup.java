package org.cacert.gigi.pages.main;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletRequest;

import org.cacert.gigi.User;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.HTMLEncoder;

public class Signup {
	User buildup = new User();
	String password;
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

	public void writeForm(PrintWriter out, ServletRequest req) {
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
						Page.translate(req, "Help on Names %sin the wiki%s"),
						"<a href=\"//wiki.cacert.org/FAQ/HowToEnterNamesInJoinForm\" target=\"_blank\">",
						"</a>"));
		t.output(out, Page.getLanguage(req), vars);
	}
	public void update(ServletRequest r) {
		buildup.setFname(r.getParameter("fname"));
		buildup.setLname(r.getParameter("lname"));
		buildup.setMname(r.getParameter("mname"));
		buildup.setSuffix(r.getParameter("suffix"));
		buildup.setEmail(r.getParameter("email"));
		general = "1".equals(r.getParameter("general"));
		country = "1".equals(r.getParameter("country"));
		regional = "1".equals(r.getParameter("regional"));
		radius = "1".equals(r.getParameter("radius"));
	}
}
