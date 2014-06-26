package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Map;

import org.cacert.gigi.Language;

public abstract class DataTable implements Outputable {
	protected abstract int getColoumnCount();

	protected abstract LinkedList<Cell> getTableContent();

	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		out.println("<table align=\"center\" valign=\"middle\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" class=\"wrapper\">");
		LinkedList<Cell> tableContnet = getTableContent();
		for (int i = 0; i < tableContnet.size() / getColoumnCount(); i++) {
			out.println("<tr>");
			for (int j = 0; j < getColoumnCount(); j++) {
				Cell current = tableContnet.get((i * getColoumnCount()) + j);
				out.println("<td " + current.getHtmlAttribs()
						+ " class=\"DataTD\">");
				out.print(current.shouldTranslate() ? l.getTranslation(current
						.getText()) : current.getText());
				out.print("</td>");
			}
			out.println("</tr>");
		}
		out.println("</table>");
	}

	/**
	 * <b>Note:</b> All cells have the html attribute class="DataTD"!
	 * 
	 * @author janis
	 * 
	 */
	public static class Cell {
		private String text, htmlAttribs;
		private boolean translate;

		public Cell() {
			this("&nbsp;", false);
		}

		public Cell(String text, boolean translate, String htmlAttribs) {
			this.text = text;
			this.translate = translate;
			this.htmlAttribs = htmlAttribs;
		}

		public Cell(String text, boolean translate) {
			this(text, translate, "");
		}

		public boolean shouldTranslate() {
			return translate;
		}

		public String getText() {
			return text;
		}

		public String getHtmlAttribs() {
			return htmlAttribs;
		}

	}

}
