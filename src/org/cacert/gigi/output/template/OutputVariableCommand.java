package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.Outputable;

final class OutputVariableCommand implements Outputable {
	private final String raw;

	OutputVariableCommand(String raw) {
		this.raw = raw;
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		Template.outputVar(out, l, vars, raw);
	}
}