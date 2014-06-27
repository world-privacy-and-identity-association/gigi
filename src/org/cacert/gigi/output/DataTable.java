package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.Language;

public class DataTable implements Outputable {
	private LinkedList<Cell> cells;
	private int columnCount;

	public DataTable(int coloumnCount, LinkedList<Cell> content) {
		this.columnCount = coloumnCount;
		this.cells = content;
	}

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		int mesCells = cells.size();
		for (Cell c : cells) {
			if (c.getColSpan() > 1) {
				mesCells += c.getColSpan();
			}
		}
		out.println("<table align=\"center\" valign=\"middle\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" class=\"wrapper\">");
		for (int i = 0; i < mesCells / columnCount; i++) {
			out.println("<tr>");
			for (int j = 0; j < columnCount;) {
				Cell current = cells.get((i * columnCount) + j);
				j += current.getColSpan();
				out.println("<td " + current.getHtmlAttribs() + " >");
				out.print(current.shouldTranslate() ? l.getTranslation(current
						.getText()) : current.getText());
				out.print("</td>");
			}
			out.println("</tr>");
		}
		out.println("</table>");
	}

	public static class Cell {
		private String text, htmlAttribs;
		private boolean translate;
		private int colSpan;

		public Cell() {
			this("&nbsp;", false);
		}

		public Cell(String text, boolean translate, int colSpan,
				String htmlAttribs) {
			this.text = text;
			this.translate = translate;
			this.htmlAttribs = htmlAttribs;
			this.colSpan = colSpan;
		}

		public Cell(String text, boolean translate) {
			this(text, translate, 1, "class=\"DataTD\"");
		}

		public Cell(String text, boolean translate, int colSpan) {
			this(text, translate, colSpan, "class=\"DataTD\"");
		}

		public boolean shouldTranslate() {
			return translate;
		}

		public String getText() {
			return text;
		}

		public int getColSpan() {
			return colSpan;
		}

		public String getHtmlAttribs() {
			return htmlAttribs;
		}

	}

}
