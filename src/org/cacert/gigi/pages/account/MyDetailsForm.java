package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.HTMLEncoder;

public class MyDetailsForm extends Form {

    private static Template assured = new Template(MyDetails.class.getResource("MyDetailsFormAssured.templ"));

    private static Template templ;
    static {
        templ = new Template(MyDetailsForm.class.getResource("MyDetailsForm.templ"));
    }

    private User target;

    private DateSelector ds;

    public MyDetailsForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
        this.ds = new DateSelector("day", "month", "year", target.getDoB());
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        try {
            if (target.getAssurancePoints() == 0) {
                String newFname = req.getParameter("fname").trim();
                String newLname = req.getParameter("lname").trim();
                String newMname = req.getParameter("mname").trim();
                String newSuffix = req.getParameter("suffix").trim();
                if (newLname.isEmpty()) {
                    throw new GigiApiException("Last name cannot be empty.");
                }
                target.setFName(newFname);
                target.setLName(newLname);
                target.setMName(newMname);
                target.setSuffix(newSuffix);
                ds.update(req);
                target.setDoB(ds.getDate());
                target.updateUserData();
            } else {
                throw new GigiApiException("No change after assurance allowed.");
            }
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
        vars.put("fname", HTMLEncoder.encodeHTML(target.getFName()));
        vars.put("mname", target.getMName() == null ? "" : HTMLEncoder.encodeHTML(target.getMName()));
        vars.put("lname", HTMLEncoder.encodeHTML(target.getLName()));
        vars.put("suffix", target.getSuffix() == null ? "" : HTMLEncoder.encodeHTML(target.getSuffix()));
        vars.put("details", "");
        if (target.getAssurancePoints() == 0) {
            vars.put("DoB", ds);
            templ.output(out, l, vars);
        } else {
            vars.put("DoB", DateSelector.getDateFormat().format(target.getDoB()));
            assured.output(out, l, vars);
        }
    }

}
