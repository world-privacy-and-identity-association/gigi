package org.cacert.gigi.output.template;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cacert.gigi.DevelLauncher;
import org.cacert.gigi.Language;
import org.cacert.gigi.output.Outputable;

public class Template implements Outputable {
	static class TemplateBlock implements Outputable {
		String[] contents;
		Outputable[] vars;

		public TemplateBlock(String[] contents, Outputable[] vars) {
			this.contents = contents;
			this.vars = vars;
		}

		@Override
		public void output(PrintWriter out, Language l, Map<String, Object> vars) {
			for (int i = 0; i < contents.length; i++) {
				out.print(contents[i]);
				if (i < this.vars.length) {
					this.vars[i].output(out, l, vars);
				}
			}
		}

	}

	TemplateBlock data;

	long lastLoaded;
	File source;

	private static final Pattern IF_PATTERN = Pattern.compile(" ?if\\(\\$([^)]+)\\) ?\\{ ?");

	public Template(URL u) {
		try {
			Reader r = new InputStreamReader(u.openStream(), "UTF-8");
			try {
				if (u.getProtocol().equals("file") && DevelLauncher.DEVEL) {
					source = new File(u.toURI());
					lastLoaded = source.lastModified() + 1000;
				}
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
			data = parse(r);
			r.close();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	public Template(Reader r) {
		try {
			data = parse(r);
			r.close();
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private TemplateBlock parse(Reader r) throws IOException {
		LinkedList<String> splitted = new LinkedList<String>();
		LinkedList<Outputable> commands = new LinkedList<Outputable>();
		StringBuffer buf = new StringBuffer();
		int ch = r.read();
		outer: while (true) {
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
			String com = buf.toString().replace("\n", "");
			buf.delete(0, buf.length());
			Matcher m = IF_PATTERN.matcher(com);
			if (m.matches()) {
				final String variable = m.group(1);
				final TemplateBlock body = parse(r);
				commands.add(new Outputable() {

					@Override
					public void output(PrintWriter out, Language l, Map<String, Object> vars) {
						Object o = vars.get(variable);
						if (o instanceof Boolean && o == Boolean.TRUE) {
							body.output(out, l, vars);
						}
					}
				});
				continue;
			}
			if (com.matches(" ?\\} ?")) {
				break;
			}
			commands.add(parseCommand(com));
		}
		splitted.add(buf.toString());
		String[] contents = splitted.toArray(new String[splitted.size()]);
		Outputable[] vars = commands.toArray(new Outputable[commands.size()]);
		return new TemplateBlock(contents, vars);
	}

	private boolean endsWith(StringBuffer buf, String string) {
		return buf.length() >= string.length()
			&& buf.substring(buf.length() - string.length(), buf.length()).equals(string);
	}

	private Outputable parseCommand(String s2) {
		if (s2.startsWith("=_")) {
			final String raw = s2.substring(2);
			return new Outputable() {

				@Override
				public void output(PrintWriter out, Language l, Map<String, Object> vars) {
					out.print(l.getTranslation(raw));
				}
			};
		} else if (s2.startsWith("=$")) {
			final String raw = s2.substring(2);
			return new Outputable() {

				@Override
				public void output(PrintWriter out, Language l, Map<String, Object> vars) {
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
				public void output(PrintWriter out, Language l, Map<String, Object> vars) {
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
		if (source != null && DevelLauncher.DEVEL) {
			if (lastLoaded < source.lastModified()) {
				try {
					System.out.println("Reloading template.... " + source);
					InputStreamReader r = new InputStreamReader(new FileInputStream(source), "UTF-8");
					parse(r);
					r.close();
					lastLoaded = source.lastModified() + 1000;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		data.output(out, l, vars);
	}

	private void outputVar(PrintWriter out, Language l, Map<String, Object> vars, String varname) {
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
