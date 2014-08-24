package org.cacert.gigi.pages.account;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Date;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.HTMLEncoder;

public class MyDetailsForm extends Form {

    private static Template templ;
    static {
        templ = new Template(new InputStreamReader(MyDetailsForm.class.getResourceAsStream("MyDetailsForm.templ")));
    }

    private User target;

    public MyDetailsForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        try {
            if (target.getAssurancePoints() == 0) {
                String newFname = req.getParameter("fname").trim();
                String newLname = req.getParameter("lname").trim();
                String newMname = req.getParameter("mname").trim();
                String newSuffix = req.getParameter("suffix").trim();
                if ((newFname.isEmpty() && !target.getFname().isEmpty()) || (newLname.isEmpty() && !target.getLname().isEmpty()) || (newMname.isEmpty() && !target.getMname().isEmpty()) || (newSuffix.isEmpty() && !target.getSuffix().isEmpty())) {
                    throw new GigiApiException("Names couldn't be removed.");
                }
                target.setFname(newFname);
                target.setLname(newLname);
                target.setMname(newMname);
                target.setSuffix(newSuffix);
                int newYear = Integer.parseInt(req.getParameter("year"));
                int newMonth = Integer.parseInt(req.getParameter("month"));
                int newDay = Integer.parseInt(req.getParameter("day"));
                Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                cal.set(Calendar.YEAR, newYear);
                cal.set(Calendar.MONTH, newMonth);
                cal.set(Calendar.DAY_OF_MONTH, newDay);
                target.setDob(new Date(cal.getTimeInMillis()));
                target.updateUserData();
            } else {
                throw new GigiApiException("No change after assurance allowed.");
            }
        } catch (SQLException e) {
            new GigiApiException(e).format(out, Page.getLanguage(req));
            return false;
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
            return false;
        } catch (NumberFormatException e) {
            new GigiApiException("Invalid value.").format(out, Page.getLanguage(req));
            return false;
        }
        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("fname", HTMLEncoder.encodeHTML(target.getFname()));
        vars.put("mname", target.getMname() == null ? "" : HTMLEncoder.encodeHTML(target.getMname()));
        vars.put("lname", HTMLEncoder.encodeHTML(target.getLname()));
        vars.put("suffix", target.getSuffix() == null ? "" : HTMLEncoder.encodeHTML(target.getSuffix()));
        DateSelector ds = new DateSelector("day", "month", "year", target.getDob());
        vars.put("DoB", ds);
        vars.put("details", "");
        templ.output(out, l, vars);
    }

}
