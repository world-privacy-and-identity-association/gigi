package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.localisation.Language;

public class CertificateValiditySelector implements Outputable {

    SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");

    private static final int DAY = 1000 * 60 * 60 * 24;

    Date from;

    String val;

    public CertificateValiditySelector() {
        fmt.setTimeZone(TimeZone.getTimeZone("UTC"));

    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print("<select name='validFrom'><option value='now'");
        if (from == null) {
            out.print(" selected='selected'");
        }
        out.print(">");
        out.print(l.getTranslation("now"));
        out.print("</option>");
        long base = getCurrentDayBase();
        for (int i = 0; i < 14; i++) {
            long date = base + DAY * i;
            String d = fmt.format(new Date(date));
            out.print("<option value='");
            out.print(d);
            out.print("'");
            if (from != null && from.getTime() == date) {
                out.print(" selected='selected'");
            }
            out.print(">");
            out.print(d);
            out.println("</option>");
        }
        out.println("</select>");

        out.print("<select name='validity'>");
        out.print("<option value='6m'");
        if ("0.5m".equals(val)) {
            out.print(" selected='selected'");
        }
        out.println(">6 months</option>");

        out.print("<option value='1y'");
        if ("1y".equals(val)) {
            out.print(" selected='selected'");
        }
        out.println(">1 year</option>");

        out.print("<option value='2y'");
        if ("2y".equals(val)) {
            out.print(" selected='selected'");
        }
        out.println(">2 years</option>");
        out.println("</select>");

        if (from == null) {
            return;
        }
        // debug dummy output
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(from);
        if ("6m".equals(val)) {
            c.add(Calendar.MONTH, 6);
        } else if ("1y".equals(val)) {
            c.add(Calendar.YEAR, 1);
        } else if ("2y".equals(val)) {
            c.add(Calendar.YEAR, 2);
        }
        out.println("From: " + fmt.format(from));
        out.println("To: " + fmt.format(c.getTime()));
    }

    private long getCurrentDayBase() {
        long base = System.currentTimeMillis();
        base -= base % DAY;
        base += DAY;
        return base;
    }

    public void update(HttpServletRequest r) {
        String from = r.getParameter("validFrom");
        if (from == null || "now".equals(from)) {
            this.from = null;
        } else {
            try {
                this.from = fmt.parse(from);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        val = r.getParameter("validity");

    }

}
