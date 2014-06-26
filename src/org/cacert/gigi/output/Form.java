package org.cacert.gigi.output;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;

public interface Form extends Outputable {
	public boolean submit(PrintWriter out, HttpServletRequest req);

}
