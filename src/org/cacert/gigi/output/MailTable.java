package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.Language;
import org.cacert.gigi.output.DataTable.Cell;

public class MailTable implements Outputable {

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		LinkedList<Cell> cells = new LinkedList<>();
		cells.add(new Cell("Email Accounts", true, 4, "class=\"title\""));
		cells.add(new Cell("Default", true));
		cells.add(new Cell("Delete", true));
		cells.add(new Cell("Status", true));
		cells.add(new Cell("Address", true));

		DataTable t = new DataTable(4, cells);
		t.output(out, l, vars);
	}



}
