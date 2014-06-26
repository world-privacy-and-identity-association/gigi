package org.cacert.gigi;

import java.io.PrintWriter;
import java.util.Map;

import org.cacert.gigi.output.Outputable;

public class Name implements Outputable {
	String fname;
	String mname;
	String lname;
	String suffix;

	public Name(String fname, String lname) {
		this.fname = fname;
		this.lname = lname;
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		out.println("<span class=\"accountdetail\">");
		out.print("<span class=\"fname\">");
		out.print(fname);
		out.print("</span> ");
		out.print("<span class=\"lname\">");
		out.print(lname);
		out.print("</span>");
		out.println("</span>");
	}
}
