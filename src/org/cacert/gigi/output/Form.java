package org.cacert.gigi.output;

import java.io.PrintWriter;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.pages.Page;

public abstract class Form implements Outputable {
	public abstract boolean submit(PrintWriter out, HttpServletRequest req);

	protected void outputError(PrintWriter out, ServletRequest req, String text) {
		out.print("<div>");
		out.print(Page.translate(req, text));
		out.println("</div>");
	}

}
