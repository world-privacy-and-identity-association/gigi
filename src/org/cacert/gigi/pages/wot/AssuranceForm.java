package org.cacert.gigi.pages.wot;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.output.Outputable;
import org.cacert.gigi.output.Template;
import org.cacert.gigi.util.HTMLEncoder;

public class AssuranceForm implements Outputable {
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
		res.put("fname", HTMLEncoder.encodeHTML(assuree.getFname()));
		res.put("mname",
				assuree.getMname() == null ? "" : HTMLEncoder
						.encodeHTML(assuree.getMname()));
		res.put("lname", HTMLEncoder.encodeHTML(assuree.getLname()));
		res.put("suffix",
				assuree.getSuffix() == null ? "" : HTMLEncoder
						.encodeHTML(assuree.getSuffix()));
		templ.output(out, l, res);
	}
}
