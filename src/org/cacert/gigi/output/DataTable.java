package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.Language;

public abstract class DataTable implements Outputable {
	protected abstract String[] getColumns();

	protected abstract LinkedList<Cell> getTableContent();

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		out.println("<table align=\"center\" valign=\"middle\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" class=\"wrapper\">");
		out.println("<tr>");
		for (String column : getColumns()) {
			out.print("<td class=\"DataTD\">");
			out.print(l.getTranslation(column));
			out.println("</td>");
		}
		out.println("</tr>");
		LinkedList<Cell> tableContnet = getTableContent();
		for (int i = 0; i < tableContnet.size() / getColumns().length; i++) {
			out.println("<tr>");
			for (int j = 0; j < getColumns().length; j++) {
				out.println("<td class=\"DataTD\">");
				Cell current = tableContnet.get((i * getColumns().length) + j);
				out.print(current.shouldTranslate() ? l.getTranslation(current
						.getText()) : current.getText());
				out.print("</td>");
			}
			out.println("</tr>");
		}
		out.println("</table>");
	}

	public static class Cell {
		private String text;
		private boolean translate;

		public Cell(String text, boolean translate) {
			this.text = text;
			this.translate = translate;
		}

		public boolean shouldTranslate() {
			return translate;
		}

		public String getText() {
			return text;
		}

	}

	public static class EmptyCell extends Cell {

		public EmptyCell() {
			super("&nbsp;", false);
		}

	}

}
