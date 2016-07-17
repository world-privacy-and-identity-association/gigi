package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.SupportedUser;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.GroupSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.PasswordResetPage;

public class SupportUserDetailsForm extends Form {

    private static final Template t = new Template(FindDomainForm.class.getResource("SupportUserDetailsForm.templ"));

    private SupportedUser user;

    private DateSelector dobSelector;

    private GroupSelector value = new GroupSelector("groupToModify");

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
        if ((req.getParameter("detailupdate") != null ? 1 : 0) + (req.getParameter("grant") != null ? 1 : 0) + (req.getParameter("deny") != null ? 1 : 0) + (req.getParameter("resetPass") != null ? 1 : 0) != 1) {
            throw new GigiApiException("More than one action requested!");
        }
        if (req.getParameter("grant") != null || req.getParameter("deny") != null) {
            value.update(req);
            Group toMod = value.getGroup();
            if (req.getParameter("grant") != null) {
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
            Language l = Language.getInstance(user.getTargetUser().getPreferredLocale());
            String method = l.getTranslation("A password reset was triggered. Please enter the required text sent to you by support on this page:");
            String subject = l.getTranslation("Password reset by support.");
            PasswordResetPage.initPasswordResetProcess(out, user.getTargetUser(), req, aword, l, method, subject);
            return true;
        }
        dobSelector.update(req);
        String fname = req.getParameter("fname");
        String mname = req.getParameter("mname");
        String lname = req.getParameter("lname");
        String suffix = req.getParameter("suffix");
        if (fname == null || mname == null || lname == null | suffix == null) {
            throw new GigiApiException("Incomplete request!");
        }
        if ( !dobSelector.isValid()) {
            throw new GigiApiException("Invalid date of birth!");
        }
        Name newName = new Name(fname, lname, mname, suffix);
        synchronized (user.getTargetUser()) {
            if (user.setDob(dobSelector.getDate()) | user.setName(newName)) {
                user.submitSupportAction();
            }
        }
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        User user = this.user.getTargetUser();
        Name name = user.getName();
        vars.put("mail", user.getEmail());
        vars.put("fname", name.getFname());
        vars.put("mname", name.getMname());
        vars.put("lname", name.getLname());
        vars.put("suffix", name.getSuffix());
        vars.put("assurer", user.canAssure());
        vars.put("dob", dobSelector);
        vars.put("assurancepoints", user.getAssurancePoints());
        vars.put("exppoints", user.getExperiencePoints());
        vars.put("id", user.getId());
        final Set<Group> gr = user.getGroups();
        vars.put("groups", new IterableDataset() {

            Iterator<Group> i = gr.iterator();

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if ( !i.hasNext()) {
                    return false;
                }
                Group g = i.next();
                vars.put("group_name", g.getName());
                return true;
            }
        });
        vars.put("groupSelector", value);
        t.output(out, l, vars);
    }

}
