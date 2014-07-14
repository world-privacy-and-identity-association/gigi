package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.Outputable;

final class IfStatement implements Outputable {
	private final String variable;
	private final TemplateBlock body;

	IfStatement(String variable, TemplateBlock body) {
		this.variable = variable;
		this.body = body;
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		Object o = vars.get(variable);
		if (!(o == Boolean.FALSE || o == null)) {
			body.output(out, l, vars);
		}
	}
}