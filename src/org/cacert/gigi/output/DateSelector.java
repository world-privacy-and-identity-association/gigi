package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Language;

public class DateSelector implements Outputable {
	String[] names;
	public DateSelector(String day, String month, String year) {
		this.names = new String[]{day, month, year};
	}
	int day;
	int month;
	int year;
	@Override
	public void output(PrintWriter out, Language l, Map<String, Object> vars) {
		out.print("<nobr><select name=\"");
		out.print(names[0]);
		out.println("\">");
		for (int i = 1; i <= 31; i++) {
			out.print("<option");
			if (i == day) {
				out.print(" selected=\"selected\"");
			}
			out.println(">" + i + "</option>");
		}
		out.println("</select>");
		SimpleDateFormat sdf = new SimpleDateFormat("MMMM", l.getLocale());
		out.print("<select name=\"");
		out.print(names[1]);
		out.println("\">");
		Calendar c = sdf.getCalendar();
		for (int i = 1; i <= 12; i++) {
			c.set(Calendar.MONTH, i - 1);
			out.print("<option value='" + i + "'");
			if (i == month) {
				out.print(" selected=\"selected\"");
			}
			out.println(">" + sdf.format(c.getTime()) + " (" + i + ")</option>");
		}
		out.println("</select>");
		out.print("<input type=\"text\" name=\"");
		out.print(names[2]);
		out.print("\" value=\"");
		if (year != 0) {
			out.print(year);
		}
		out.print("\" size=\"4\" autocomplete=\"off\"></nobr>");
	}

	public void update(HttpServletRequest r) {
		String dayS = r.getParameter(names[0]);
		if (dayS != null) {
			day = parseIntSafe(dayS);
		}

		String monthS = r.getParameter(names[1]);
		if (monthS != null) {
			month = parseIntSafe(monthS);
		}

		String yearS = r.getParameter(names[2]);
		if (yearS != null) {
			year = parseIntSafe(yearS);
		}
	}
	private int parseIntSafe(String dayS) {
		try {
			return Integer.parseInt(dayS);
		} catch (NumberFormatException e) {

		}
		return 0;
	}
	public boolean isValid() {
		if (!(1900 < year && 1 <= month && month <= 12 && 1 <= day && day <= 32)) {
			return false;
		}
		return true; // TODO checkdate
	}

	@Override
	public String toString() {
		return "DateSelector [names=" + Arrays.toString(names) + ", day=" + day
				+ ", month=" + month + ", year=" + year + "]";
	}

}
