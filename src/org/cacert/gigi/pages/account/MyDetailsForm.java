package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.ArrayIterable;
import org.cacert.gigi.output.CountrySelector;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.GroupIterator;
import org.cacert.gigi.output.GroupSelector;
import org.cacert.gigi.output.NameInput;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

public class MyDetailsForm extends Form {

    private static final Template assured = new Template(MyDetails.class.getResource("MyDetailsFormAssured.templ"));

    private static final Template templ = new Template(MyDetailsForm.class.getResource("MyDetailsForm.templ"));

    private static final Template names = new Template(MyDetailsForm.class.getResource("NamesForm.templ"));

    private static final Template roles = new Template(MyDetailsForm.class.getResource("MyDetailsRoles.templ"));

    private User target;

    private DateSelector ds;

    private NameInput ni;

    private CountrySelector cs;

    private GroupSelector selectedGroup = new GroupSelector("groupToModify", false);

    public MyDetailsForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
        ni = new NameInput();

        this.ds = new DateSelector("day", "month", "year", target.getDoB());

        if (target.getResidenceCountry() == null) {
            this.cs = new CountrySelector("residenceCountry", true);
        } else {
            this.cs = new CountrySelector("residenceCountry", true, target.getResidenceCountry());
        }
    }

    @Override
    public boolean submit(HttpServletRequest req) throws GigiApiException {
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
            if ("updateResidenceCountry".equals(action)) {
                cs.update(req);
                target.setResidenceCountry(cs.getCountry());
            }

            if ("addGroup".equals(action) || "removeGroup".equals(action)) {
                selectedGroup.update(req);
                Group toMod = selectedGroup.getGroup();
                if ("addGroup".equals(action)) {
                    target.grantGroup(target, toMod);
                } else {
                    target.revokeGroup(target, toMod);
                }
                return true;
            }

        } catch (NumberFormatException e) {
            throw new GigiApiException("Invalid value.");
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

        vars.put("residenceCountry", cs);
        if (target.getReceivedAssurances().length == 0) {
            vars.put("DoB", ds);
            templ.output(out, l, vars);
        } else {
            vars.put("DoB", target.getDoB());
            assured.output(out, l, vars);
        }

        final Set<Group> gr = target.getGroups();
        vars.put("support-groups", new GroupIterator(gr.iterator(), true));
        vars.put("groups", new GroupIterator(gr.iterator(), false));
        vars.put("groupSelector", selectedGroup);
        roles.output(out, l, vars);
    }

}
