package org.cacert.gigi.pages.wot;

import java.io.InputStreamReader;
import java.io.PrintWriter;
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

	@Override
	public boolean submit(PrintWriter out, HttpServletRequest req) {
		if (!"1".equals(req.getAttribute("certify"))) {
			// s

		}
		return false;
	}
}
