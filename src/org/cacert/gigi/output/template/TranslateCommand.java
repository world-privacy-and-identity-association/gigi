package org.cacert.gigi.output.template;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.Outputable;

final class TranslateCommand implements Outputable {
	private final String raw;

	TranslateCommand(String raw) {
		this.raw = raw;
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		out.print(l.getTranslation(raw));
	}
}