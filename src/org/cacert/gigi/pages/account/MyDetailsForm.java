package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.ArrayIterable;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.NameInput;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class MyDetailsForm extends Form {

    private static final Template assured = new Template(MyDetails.class.getResource("MyDetailsFormAssured.templ"));

    private static final Template templ = new Template(MyDetailsForm.class.getResource("MyDetailsForm.templ"));

    private static final Template names = new Template(MyDetailsForm.class.getResource("NamesForm.templ"));

    private User target;

    private DateSelector ds;

    private NameInput ni;

    public MyDetailsForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
        ni = new NameInput();

        this.ds = new DateSelector("day", "month", "year", target.getDoB());
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        try {
            String rn = req.getParameter("removeName");
            if (rn != null) {
                Name n = Name.getById(Integer.parseInt(rn));
                if (n.getOwner() != target) {
                    throw new GigiApiException("Cannot remove a name that does not belong to this account.");
                }
                if (n.equals(target.getPreferredName())) {
                    throw new GigiApiException("Cannot remove the account's preferred name.");
                }
                n.remove();
                return true;
            }
            String dn = req.getParameter("deprecateName");
            if (dn != null) {
                Name n = Name.getById(Integer.parseInt(dn));
                if (n.getOwner() != target) {
                    throw new GigiApiException("Cannot deprecate a name that does not belong to this account.");
                }
                if (n.equals(target.getPreferredName())) {
                    throw new GigiApiException("Cannot deprecate the account's preferred name.");
                }
                n.deprecate();
                return true;
            }
            String pn = req.getParameter("preferred");
            if (pn != null) {
                Name n = Name.getById(Integer.parseInt(pn));
                target.setPreferredName(n);
                return true;
            }

            String action = req.getParameter("action");
            if ("addName".equals(action)) {
                ni.update(req);
                ni.createName(target);
                return true;
            }
            if ("updateDoB".equals(action)) {
                ds.update(req);
                target.setDoB(ds.getDate());
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
        vars.put("exNames", new ArrayIterable<Name>(target.getNames()) {

            Name preferred = target.getPreferredName();

            @Override
            public void apply(Name t, Language l, Map<String, Object> vars) {
                if (t.equals(preferred)) {
                    vars.put("preferred", " disabled");
                    vars.put("deprecated", " disabled");
                } else {
                    if (t.isDeprecated()) {
                        vars.put("deprecated", " disabled");
                    } else {
                        vars.put("deprecated", "");
                    }
                    vars.put("preferred", "");
                }
                vars.put("name", t);
                vars.put("id", t.getId());
                vars.put("npoints", Integer.toString(t.getAssurancePoints()));
            }

        });
        vars.put("name", ni);
        names.output(out, l, vars);
        if (target.getReceivedAssurances().length == 0) {
            vars.put("DoB", ds);
            templ.output(out, l, vars);
        } else {
            vars.put("DoB", target.getDoB());
            assured.output(out, l, vars);
        }
    }

}
