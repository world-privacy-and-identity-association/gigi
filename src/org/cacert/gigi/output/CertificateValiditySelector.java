package org.cacert.gigi.output;

import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.sql.Date;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.util.HTMLEncoder;

public class CertificateValiditySelector implements Outputable {

    private static ThreadLocal<SimpleDateFormat> fmt = new ThreadLocal<>();

    private static final long DAY = 1000 * 60 * 60 * 24;

    private Date from;

    private String val = "2y";

    public CertificateValiditySelector() {

    }

    public static SimpleDateFormat getDateFormat() {
        SimpleDateFormat local = fmt.get();
        if (local == null) {
            local = new SimpleDateFormat("yyyy-MM-dd");
            local.setTimeZone(TimeZone.getTimeZone("UTC"));
            fmt.set(local);
        }
        return local;
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
            String d = getDateFormat().format(new Date(date));
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

        out.print("<input type='text' name='validity' value='");
        out.print(HTMLEncoder.encodeHTML(val));
        out.println("'>");

        if (from == null) {
            return;
        }

    }

    private long getCurrentDayBase() {
        long base = System.currentTimeMillis();
        base -= base % DAY;
        base += DAY;
        return base;
    }

    public void update(HttpServletRequest r) throws GigiApiException {
        String from = r.getParameter("validFrom");

        GigiApiException gae = new GigiApiException();
        try {
            saveStartDate(from);
        } catch (GigiApiException e) {
            gae.mergeInto(e);
        }
        try {
            String validity = r.getParameter("validity");
            if (validity != null) {
                checkValidityLength(validity);
                val = validity;
            }
        } catch (GigiApiException e) {
            gae.mergeInto(e);
        }
        if ( !gae.isEmpty()) {
            throw gae;
        }

    }

    public static void checkValidityLength(String newval) throws GigiApiException {
        if (newval.endsWith("y") || newval.endsWith("m")) {
            if (newval.length() > 10) { // for database
                throw new GigiApiException("The validity interval entered is invalid.");
            }
            String num = newval.substring(0, newval.length() - 1);
            try {
                int len = Integer.parseInt(num);
                if (len <= 0) {
                    throw new GigiApiException("The validity interval entered is invalid.");
                }
            } catch (NumberFormatException e) {
                throw new GigiApiException("The validity interval entered is invalid.");
            }
        } else {
            try {
                getDateFormat().parse(newval);
            } catch (ParseException e) {
                throw new GigiApiException("The validity interval entered is invalid.");
            }
        }
    }

    private void saveStartDate(String from) throws GigiApiException {
        if (from == null || "now".equals(from)) {
            this.from = null;
        } else {
            try {
                this.from = new Date(getDateFormat().parse(from).getTime());
            } catch (ParseException e) {
                throw new GigiApiException("The validity start date entered is invalid.");
            }
        }
    }

    public Date getFrom() {
        return from;
    }

    public String getTo() {
        return val;
    }

}
