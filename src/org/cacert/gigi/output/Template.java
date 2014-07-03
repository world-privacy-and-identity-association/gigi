package org.cacert.gigi.output;

import java.io.EOFException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.Language;

public class Template implements Outputable {
	String[] contents;
	Outputable[] vars;

	public Template(Reader r) {
		try {
			LinkedList<String> splitted = new LinkedList<String>();
			LinkedList<Outputable> commands = new LinkedList<Outputable>();
			StringBuffer buf = new StringBuffer();
			int ch = r.read();
			outer : while (true) {
				while (!endsWith(buf, "<?")) {
					if (ch == -1) {
						break outer;
					}
					buf.append((char) ch);
					ch = r.read();
				}
				buf.delete(buf.length() - 2, buf.length());
				splitted.add(buf.toString());
				buf.delete(0, buf.length());
				while (!endsWith(buf, "?>")) {
					buf.append((char) ch);
					ch = r.read();
					if (ch == -1) {
						throw new EOFException();
					}
				}
				buf.delete(buf.length() - 2, buf.length());
				commands.add(parseCommand(buf.toString()));
				buf.delete(0, buf.length());
			}
			splitted.add(buf.toString());
			contents = splitted.toArray(new String[splitted.size()]);
			vars = commands.toArray(new Outputable[commands.size()]);
		} catch (IOException e) {
			throw new Error(e);
		}
	}
	private boolean endsWith(StringBuffer buf, String string) {
		return buf.length() >= string.length()
				&& buf.substring(buf.length() - string.length(), buf.length())
						.equals(string);
	}
	private Outputable parseCommand(String s2) {
		if (s2.startsWith("=_")) {
			final String raw = s2.substring(2);
			return new Outputable() {

				@Override
				public void output(PrintWriter out, Language l,
						Map<String, Object> vars) {
					out.print(l.getTranslation(raw));
				}
			};
		} else if (s2.startsWith("=$")) {
			final String raw = s2.substring(2);
			return new Outputable() {

				@Override
				public void output(PrintWriter out, Language l,
						Map<String, Object> vars) {
					outputVar(out, l, vars, raw);
				}
			};
		} else if (s2.startsWith("=s,")) {
			String command = s2.substring(3);
			final LinkedList<String> store = new LinkedList<String>();
			while (command.startsWith("$")) {
				int idx = command.indexOf(",");
				store.add(command.substring(0, idx));
				command = command.substring(idx + 1);
			}
			final String text = command;
			return new Outputable() {

				@Override
				public void output(PrintWriter out, Language l,
						Map<String, Object> vars) {
					String[] parts = l.getTranslation(text).split("%s");
					String[] myvars = store.toArray(new String[store.size()]);
					out.print(parts[0]);
					for (int j = 1; j < parts.length; j++) {
						outputVar(out, l, vars, myvars[j - 1].substring(1));
						out.print(parts[j]);
					}
				}
			};
		} else {
			System.out.println("Unknown processing instruction: " + s2);
		}
		return null;
	}
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		for (int i = 0; i < contents.length; i++) {
			out.print(contents[i]);
			if (i < this.vars.length) {
				this.vars[i].output(out, l, vars);
			}
		}
	}
	private void outputVar(PrintWriter out, Language l,
			Map<String, Object> vars, String varname) {
		Object s = vars.get(varname);

		if (s == null) {
			System.out.println("Empty variable: " + varname);
		}
		if (s instanceof Outputable) {
			((Outputable) s).output(out, l, vars);
		} else {
			out.print(s);
		}
	}
}
