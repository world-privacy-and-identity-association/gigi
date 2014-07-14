package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.Outputable;

final class ForeachStatement implements Outputable {
	private final String variable;
	private final TemplateBlock body;

	ForeachStatement(String variable, TemplateBlock body) {
		this.variable = variable;
		this.body = body;
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		Object o = vars.get(variable);
		if (o instanceof IterableDataset) {
			IterableDataset id = (IterableDataset) o;
			Map<String, Object> subcontext = new HashMap<String, Object>(vars);
			while (id.next(l, subcontext)) {
				body.output(out, l, subcontext);
			}
		}
	}
}