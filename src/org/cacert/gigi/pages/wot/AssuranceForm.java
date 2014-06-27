package org.cacert.gigi.pages.wot;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.Template;

public class AssuranceForm extends Form {
	User assuree;
	static final Template templ;
	static {
		templ = new Template(new InputStreamReader(
				AssuranceForm.class.getResourceAsStream("AssuranceForm.templ")));
	}

	public AssuranceForm(int assuree) {
		this.assuree = new User(assuree);
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		HashMap<String, Object> res = new HashMap<String, Object>();
		res.putAll(vars);
		res.put("name", assuree.getName());
		templ.output(out, l, res);
	}
	SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-dd");

	@Override
	public boolean submit(PrintWriter out, HttpServletRequest req) {
		out.println("<div class='formError'>");
		boolean failed = false;

		if (!"1".equals(req.getParameter("certify"))
				|| !"1".equals(req.getParameter("rules"))
				|| !"1".equals(req.getParameter("CCAAgreed"))
				|| !"1".equals(req.getParameter("assertion"))) {
			outputError(out, req, "You failed to check all boxes to validate"
					+ " your adherence to the rules and policies of CAcert");
			failed = true;

		}
		if (req.getParameter("date") == null
				|| req.getParameter("date").equals("")) {
			outputError(out, req,
					"You must enter the date when you met the assuree.");
			failed = true;
		} else {
			try {
				Date d = sdf.parse(req.getParameter("date"));
				if (d.getTime() > System.currentTimeMillis()) {
					outputError(out, req,
							"You must not enter a date in the future.");
					failed = true;
				}
			} catch (ParseException e) {
				outputError(out, req,
						"You must enter the date in this format: YYYY-MM-DD.");
				failed = true;
			}
		}
		// check location, min 3 characters
		if (req.getParameter("location") == null
				|| req.getParameter("location").equals("")) {
			outputError(out, req,
					"You failed to enter a location of your meeting.");
			failed = true;
		} else if (req.getParameter("location").length() <= 2) {
			outputError(out, req,
					"You must enter a location with at least 3 characters eg town and country.");
			failed = true;
		}
		// TODO checkPoints
		out.println("</div>");
		if (failed) {
			return false;
		}

		return false;
	}
}
