package org.cacert.gigi.api;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GigiAPI extends HttpServlet {
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String pi = req.getPathInfo();
		if (pi == null) {
			return;
		}
		if (pi.equals("/security/csp/report")) {
			ServletInputStream sis = req.getInputStream();
			InputStreamReader isr = new InputStreamReader(sis, "UTF-8");
			StringBuffer strB = new StringBuffer();
			char[] buffer = new char[4 * 1024];
			int len;
			while ((len = isr.read(buffer)) > 0) {
				strB.append(buffer, 0, len);
			}
			System.out.println(strB);
		}
	}
}
