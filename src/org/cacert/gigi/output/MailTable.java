package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.DataTable.Cell;

public class MailTable implements Outputable {
	private String resultSet, userMail;

	public MailTable(String key, String userMail) {
		this.resultSet = key;
		this.userMail = userMail;
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		ResultSet rs = (ResultSet) vars.get(resultSet);
		String userMail = (String) vars.get(this.userMail);
		LinkedList<Cell> cells = new LinkedList<>();
		cells.add(new Cell("Email Accounts", true, 4, "class=\"title\""));
		cells.add(new Cell("Default", true));
		cells.add(new Cell("Status", true));
		cells.add(new Cell("Delete", true));
		cells.add(new Cell("Address", true));
		try {
			rs.beforeFirst();
			while (rs.next()) {
				cells.add(new Cell());
				cells.add(new Cell(
						rs.getString("hash").trim().isEmpty() ? "Verified"
								: "Unverified", true));
				if (rs.getString("email").equals(userMail)) {
					cells.add(new Cell(
							"N/A"
									, true));
				} else {
					cells.add(new Cell("<input type=\"checkbox\" name=\"delid[]\" value=\""
											+ rs.getInt("id") + "\">", false));
				}
				cells.add(new Cell(rs.getString("email"), false));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		String trans = l.getTranslation("Make Default");
		cells.add(new Cell(
				"<input type=\"submit\" name=\"makedefault\" value=\"" + trans
						+ "\">", false, 2));
		trans = l.getTranslation("Delete");
		cells.add(new Cell("<input type=\"submit\" name=\"process\" value=\""
				+ trans + "\">", false, 2));
		DataTable t = new DataTable(4, cells);
		t.output(out, l, vars);
	}

}
