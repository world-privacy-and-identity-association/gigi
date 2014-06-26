package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.io.Reader;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.cacert.gigi.Language;

public class Template implements Outputable {
	String[] contents;

	public Template(Reader r) {
		LinkedList<String> splitted = new LinkedList<String>();
		Scanner sc = new Scanner(r);
		Pattern p1 = Pattern.compile("([^<]|<[^?])*<\\?");
		Pattern p2 = Pattern.compile("([^<]|<[^?])*\\?>");
		while (true) {
			String s1 = sc.findWithinHorizon(p1, 0);
			if (s1 == null) {
				break;
			}
			s1 = s1.substring(0, s1.length() - 2);
			splitted.add(s1);
			String s2 = sc.findWithinHorizon(p2, 0);
			s2 = s2.substring(0, s2.length() - 2);
			splitted.add(s2);
		}
		sc.useDelimiter("\0");
		if (sc.hasNext()) {
			splitted.add(sc.next());
		}
		sc.close();
		contents = splitted.toArray(new String[splitted.size()]);
	}
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		LinkedList<String> store = new LinkedList<String>();
		for (int i = 0; i < contents.length; i++) {
			if (i % 2 == 0) {
				out.print(contents[i]);
			} else if (contents[i].startsWith("=_")) {
				out.print(l.getTranslation(contents[i].substring(2)));
			} else if (contents[i].startsWith("=$")) {
				outputVar(out, l, vars, contents[i].substring(2));
			} else if (contents[i].startsWith("=s,")) {
				String command = contents[i].substring(3);
				store.clear();
				while (command.startsWith("$")) {
					int idx = command.indexOf(",");
					store.add(command.substring(0, idx));
					command = command.substring(idx + 1);
				}
				String[] parts = l.getTranslation(command).split("%s");
				String[] myvars = store.toArray(new String[store.size()]);
				out.print(parts[0]);
				for (int j = 1; j < parts.length; j++) {
					outputVar(out, l, vars, myvars[j - 1].substring(1));
					out.print(parts[j]);
				}
			} else {
				System.out.println("Unknown processing instruction: "
						+ contents[i]);
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
