package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.util.CalendarUtil;
import club.wpia.gigi.util.DayDate;
import club.wpia.gigi.util.HTMLEncoder;

public class DateSelector implements Outputable {

    private String[] names;

    public DateSelector(String day, String month, String year, DayDate date) {
        this(day, month, year);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(date.getTime());
        this.day = cal.get(Calendar.DAY_OF_MONTH);
        this.month = cal.get(Calendar.MONTH) + 1;
        this.year = cal.get(Calendar.YEAR);
    }

    public DateSelector(String day, String month, String year) {
        this.names = new String[] {
                HTMLEncoder.encodeHTML(day), HTMLEncoder.encodeHTML(month), HTMLEncoder.encodeHTML(year)
        };
    }

    private int day;

    private int month;

    private int year;

    private static ThreadLocal<SimpleDateFormat> fmt = new ThreadLocal<>();

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print("<nobr>");
        outputYear(out);
        outputMonth(out, l);
        outputDay(out);
        out.print("</nobr>");
    }

    private void outputDay(PrintWriter out) {
        out.print("<select name=\"");
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
    }

    private void outputMonth(PrintWriter out, Language l) {
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
    }

    private void outputYear(PrintWriter out) {
        out.print("<input type=\"text\" name=\"");
        out.print(names[2]);
        out.print("\" value=\"");
        if (year != 0) {
            out.print(year);
        }
        out.println("\" size=\"4\" autocomplete=\"off\">");
    }

    public void update(HttpServletRequest r) throws GigiApiException {
        try {
            String dayS = r.getParameter(names[0]);
            if (dayS != null) {
                day = Integer.parseInt(dayS);
            }

            String monthS = r.getParameter(names[1]);
            if (monthS != null) {
                month = Integer.parseInt(monthS);
            }

            String yearS = r.getParameter(names[2]);
            if (yearS != null) {
                year = Integer.parseInt(yearS);
            }
        } catch (NumberFormatException e) {
            throw new GigiApiException("Unparsable date.");
        }
    }

    public boolean isValid() {
        if ( !(1890 < year && 1 <= month && month <= 12 && 1 <= day && day <= 32)) {
            return false;
        }

        if ( !CalendarUtil.isDateValid(year, month, day)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "DateSelector [names=" + Arrays.toString(names) + ", day=" + day + ", month=" + month + ", year=" + year + "]";
    }

    public DayDate getDate() {
        return CalendarUtil.getDateFromComponents(year, month, day);
    }

    public static SimpleDateFormat getDateFormat() {
        SimpleDateFormat local = fmt.get();
        if (local == null) {
            local = new SimpleDateFormat("yyyy-MM-dd");
            local.setLenient(false);
            local.setTimeZone(TimeZone.getTimeZone("UTC"));
            fmt.set(local);
        }
        return local;
    }

}
