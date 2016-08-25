package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.SupportedUser;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.ArrayIterable;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.GroupIterator;
import org.cacert.gigi.output.GroupSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

public class SupportUserDetailsForm extends Form {

    private static final Template t = new Template(FindUserByDomainForm.class.getResource("SupportUserDetailsForm.templ"));

    private SupportedUser user;

    private DateSelector dobSelector;

    private GroupSelector value = new GroupSelector("groupToModify", true);

    public SupportUserDetailsForm(HttpServletRequest hsr, SupportedUser user) {
        super(hsr);
        this.user = user;
        dobSelector = new DateSelector("dobd", "dobm", "doby", user.getTargetUser().getDoB());
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        if (user.getTicket() == null) {
            return false;
        }
        if ((req.getParameter("detailupdate") != null ? 1 : 0) + (req.getParameter("addGroup") != null ? 1 : 0) + (req.getParameter("removeGroup") != null ? 1 : 0) + (req.getParameter("resetPass") != null ? 1 : 0) != 1) {
            throw new GigiApiException("More than one action requested!");
        }
        if (req.getParameter("addGroup") != null || req.getParameter("removeGroup") != null) {
            value.update(req);
            Group toMod = value.getGroup();
            if (req.getParameter("addGroup") != null) {
                user.grant(toMod);
            } else {
                user.revoke(toMod);
            }
            return true;
        }
        if (req.getParameter("resetPass") != null) {
            String aword = req.getParameter("aword");
            if (aword == null || aword.equals("")) {
                throw new GigiApiException("An A-Word is required to perform a password reset.");
            }
            user.triggerPasswordReset(aword, out, req);
            return true;
        }
        dobSelector.update(req);
        if ( !dobSelector.isValid()) {
            throw new GigiApiException("Invalid date of birth!");
        }
        user.setDob(dobSelector.getDate());
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        User user = this.user.getTargetUser();
        vars.put("mail", user.getEmail());
        vars.put("status", l.getTranslation(user.isValidEmail(user.getEmail()) ? "verified" : "not verified"));
        vars.put("exNames", new ArrayIterable<Name>(user.getNames()) {

            @Override
            public void apply(Name t, Language l, Map<String, Object> vars) {
                vars.put("name", t);
                vars.put("points", Integer.toString(t.getAssurancePoints()));
            }

        });
        vars.put("assurer", user.canAssure());
        vars.put("dob", dobSelector);
        vars.put("assurancepoints", user.getAssurancePoints());
        vars.put("exppoints", user.getExperiencePoints());
        final Set<Group> gr = user.getGroups();
        vars.put("support-groups", new GroupIterator(gr.iterator(), true));
        vars.put("groups", new GroupIterator(gr.iterator(), false));
        vars.put("groupSelector", value);
        t.output(out, l, vars);
    }

}
