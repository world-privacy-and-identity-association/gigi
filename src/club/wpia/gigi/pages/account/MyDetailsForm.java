package club.wpia.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.ArrayIterable;
import club.wpia.gigi.output.CountrySelector;
import club.wpia.gigi.output.DateSelector;
import club.wpia.gigi.output.GroupIterator;
import club.wpia.gigi.output.GroupSelector;
import club.wpia.gigi.output.NameInput;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;

public class MyDetailsForm extends Form {

    private static final Template verified = new Template(MyDetails.class.getResource("MyDetailsFormVerified.templ"));

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
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
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
                return new RedirectResult(MyDetails.PATH);
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
                return new RedirectResult(MyDetails.PATH);
            }
            String pn = req.getParameter("preferred");
            if (pn != null) {
                Name n = Name.getById(Integer.parseInt(pn));
                target.setPreferredName(n);
                return new RedirectResult(MyDetails.PATH);
            }

            String action = req.getParameter("action");
            if ("addName".equals(action)) {
                ni.update(req);
                ni.createName(target);
                return new RedirectResult(MyDetails.PATH);
            } else if ("updateDoB".equals(action)) {
                ds.update(req);
                target.setDoB(ds.getDate());
                return new RedirectResult(MyDetails.PATH);
            } else if ("updateResidenceCountry".equals(action)) {
                cs.update(req);
                target.setResidenceCountry(cs.getCountry());
                return new RedirectResult(MyDetails.PATH);
            } else if ("addGroup".equals(action) || "removeGroup".equals(action)) {
                selectedGroup.update(req);
                Group toMod = selectedGroup.getGroup();
                if ("addGroup".equals(action)) {
                    target.grantGroup(target, toMod);
                } else {
                    target.revokeGroup(target, toMod);
                }
                return new RedirectResult(MyDetails.PATH);
            } else {
                throw new GigiApiException("Invalid action.");
            }

        } catch (NumberFormatException e) {
            throw new GigiApiException("Invalid value.");
        }
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
                vars.put("npoints", Integer.toString(t.getVerificationPoints()));
            }

        });
        vars.put("name", ni);
        names.output(out, l, vars);

        vars.put("residenceCountry", cs);
        if (target.getReceivedVerifications().length == 0) {
            vars.put("DoB", ds);
            templ.output(out, l, vars);
        } else {
            vars.put("DoB", target.getDoB());
            verified.output(out, l, vars);
        }

        final Set<Group> gr = target.getGroups();
        vars.put("support-groups", new GroupIterator(gr.iterator(), true));
        vars.put("groups", new GroupIterator(gr.iterator(), false));
        vars.put("groupSelector", selectedGroup);
        roles.output(out, l, vars);
    }

}
