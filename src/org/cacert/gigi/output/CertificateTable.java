package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.pages.account.Certificates;

public class CertificateTable implements Outputable {

    String resultSet;

    public CertificateTable(String resultSet) {
        this.resultSet = resultSet;
    }

    private static final String[] columnNames = new String[] {
            "Renew/Revoke/Delete", "Status", "Email Address", "SerialNumber", "Revoked", "Expires", "Login"
    };

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        ResultSet rs = (ResultSet) vars.get(resultSet);
        try {
            out.println("<form method=\"post\">");
            out.println("<table class=\"wrapper dataTable\">");
            out.println("<thead><tr>");
            for (String column : columnNames) {
                out.print("<td>");
                out.print(l.getTranslation(column));
                out.println("</td>");
            }
            out.print("<td colspan=\"2\">");
            out.print(l.getTranslation("Comment *"));
            out.println("</td></tr></thead><tbody>");

            rs.beforeFirst();
            while (rs.next()) {
                // out.println(rs.getString("id"));
                out.print("<tr><td>&nbsp;</td><td>State</td><td>");
                out.println(rs.getString("CN"));
                out.print("</td><td><a href='");
                out.print(Certificates.PATH);
                out.print("/");
                out.print(rs.getString("serial"));
                out.print("'>");
                out.println(rs.getString("serial"));
                out.print("</a></td><td>");
                if (rs.getString("revoked") == null) {
                    out.println("N/A");
                } else {
                    out.println(rs.getString("revoked"));
                }
                out.print("</td><td>");
                out.println(rs.getString("expire"));
                out.println("</td><td>a</td><td>a</td></tr>");
            }
            out.println("</tbody></table>");
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
