package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.DataTable.Cell;

public class CertificateTable implements Outputable {
	String resultSet;
	public CertificateTable(String resultSet) {
		this.resultSet = resultSet;
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		ResultSet rs = (ResultSet) vars.get(resultSet);
		try {
			out.println("<form method=\"post\" action=\"account.php\">");
			final LinkedList<Cell> cells = new LinkedList<>();
			cells.add(new Cell("Renew/Revoke/Delete", true));
			cells.add(new Cell("Status", true));
			cells.add(new Cell("Email Address", true));
			cells.add(new Cell("SerialNumber", true));
			cells.add(new Cell("Revoked", true));
			cells.add(new Cell("Expires", true));
			cells.add(new Cell("Login", true));
			cells.add(new Cell("Comment *", true, "colspan=\"2\""));
			rs.beforeFirst();
			while (rs.next()) {
				// out.println(rs.getString("id"));
				cells.add(new Cell());
				cells.add(new Cell("State", false));
				cells.add(new Cell(rs.getString("CN"), false));
				cells.add(new Cell(rs.getString("serial"), false));
				if (rs.getString("revoked") == null) {
					cells.add(new Cell("N/A", false));
				} else {
					cells.add(new Cell(rs.getString("revoked"), false));
				}
				cells.add(new Cell(rs.getString("expire"), false));
				cells.add(new Cell(rs.getString("a"), false));
				cells.add(new Cell(rs.getString("a"), false));
			}
			DataTable t = new DataTable() {

				@Override
				protected LinkedList<Cell> getTableContent() {
					return cells;
				}

				@Override
				protected int getColoumnCount() {
					return 8;
				}
			};
			t.output(out, l, vars);
			out.println("</form>");
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
