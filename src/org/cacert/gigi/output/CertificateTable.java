package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.cacert.gigi.Language;

public class CertificateTable implements Outputable {
	String resultSet;
	public CertificateTable(String resultSet) {
		this.resultSet = resultSet;
	}
	private static final String[] columnNames = new String[]{
			"Renew/Revoke/Delete", "Status", "Email Address", "SerialNumber",
			"Revoked", "Expires", "Login"};

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		ResultSet rs = (ResultSet) vars.get(resultSet);
		try {
			out.println("<form method=\"post\" action=\"account.php\">");
			out.println("<table align=\"center\" valign=\"middle\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" class=\"wrapper\">");
			out.println("<tr>");
			for (String column : columnNames) {
				out.print("<td class=\"DataTD\">");
				out.print(l.getTranslation(column));
				out.println("</td>");
			}
			out.print("<td colspan=\"2\" class=\"DataTD\">");
			out.print(l.getTranslation("Comment *"));
			out.println("</td></tr>");

			rs.beforeFirst();
			while (rs.next()) {
				// out.println(rs.getString("id"));
				out.print("<tr><td class=\"DataTD\">&nbsp;</td><td class=\"DataTD\">State</td><td class=\"DataTD\">");
				out.println(rs.getString("CN"));
				out.print("</td><td class=\"DataTD\">");
				out.println(rs.getString("serial"));
				out.print("</td><td class=\"DataTD\">");
				if (rs.getString("revoked") == null) {
					out.println("N/A");
				} else {
					out.println(rs.getString("revoked"));
				}
				out.print("</td><td class=\"DataTD\">");
				out.println(rs.getString("expire"));
				out.println("</td><td class=\"DataTD\">a</td><td class=\"DataTD\">a</td></tr>");
			}
			out.println("</table>");
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
