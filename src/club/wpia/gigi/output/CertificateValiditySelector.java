package club.wpia.gigi.output;

import java.io.PrintWriter;
import java.sql.Date;
import java.text.ParseException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Outputable;
import club.wpia.gigi.util.HTMLEncoder;

public class CertificateValiditySelector implements Outputable {

    private static final long DAY = 1000 * 60 * 60 * 24;

    private Date from;

    private String val = "2y";

    public CertificateValiditySelector() {

    }

    @Override
    public void output(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print("<label for='validFrom'>");
        out.println(l.getTranslation("start: "));
        out.print("</label><select name='validFrom'><option value='now'");
        if (from == null) {
            out.print(" selected='selected'");
        }
        out.print(">");
        out.print(l.getTranslation("now"));
        out.print("</option>");
        long base = getCurrentDayBase();
        for (int i = 0; i < 14; i++) {
            long date = base + DAY * i;
            String d = DateSelector.getDateFormat().format(new Date(date));
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

        out.print("<label for='validity'>");
        out.println(l.getTranslation("end: "));
        out.print("</label>");
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
                DateSelector.getDateFormat().parse(newval);
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
                this.from = new Date(DateSelector.getDateFormat().parse(from).getTime());
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
