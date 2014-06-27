package org.cacert.gigi.pages.account;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.DataTable;
import org.cacert.gigi.output.DataTable.Cell;
import org.cacert.gigi.pages.Page;

public class MailAdd extends Page{
	public static final String DEFAULT_PATH = "/account/mails/add";
	public MailAdd(String title) {
		super(title);
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		LinkedList<Cell> cells = new LinkedList<>();
		cells.add(new Cell("Add Email", true, 2, "class=\"title\""));
		cells.add(new Cell("Email Address", true));
		cells.add(new Cell("<input type=\"text\" name=\"newemail\">", false));
		Language language = getLanguage(req);
		String trans = language.getTranslation("I own or am authorised to control this email address");
		cells.add(new Cell("<input type=\"submit\" name=\"process\" value=\""
				+ trans + "\">", false, 2));
		DataTable dt = new DataTable(2, cells);
		dt.output(resp.getWriter(), language);
		PrintWriter out = resp.getWriter();
		out.println("<p>");
		out.println(language
				.getTranslation(
						"Currently we only issue certificates for Punycode domains if the person requesting them has code signing attributes attached to their account, as these have potentially slightly higher security risk."));
		out.println("</p>");
	}

}
