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
import org.cacert.gigi.output.ArrayIterable;
import org.cacert.gigi.output.DateSelector;
import org.cacert.gigi.output.GroupSelector;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Outputable;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.output.template.TranslateCommand;
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
            String actionType = "granted";
            value.update(req);
            Group toMod = value.getGroup();
            if (req.getParameter("grant") != null) {
                user.grant(toMod);
            } else {
                actionType = "revoked";
                user.revoke(toMod);
            }
            String subject = "Change Group Permissions";
            Outputable message = SprintfCommand.createSimple("The group permission {0} was {1}.", toMod.getDatabaseName(), actionType);
            user.sendSupportNotification(subject, message);
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
            Outputable message = new TranslateCommand("A password reset was triggered and an email was sent to user.");
            user.sendSupportNotification(subject, message);
            return true;
        }
        dobSelector.update(req);
        if ( !dobSelector.isValid()) {
            throw new GigiApiException("Invalid date of birth!");
        }
        user.setDob(dobSelector.getDate());

        String subject = "Change Account Data";
        Outputable message = new TranslateCommand("The account data was changed.");
        user.sendSupportNotification(subject, message);
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        User user = this.user.getTargetUser();
        vars.put("mail", user.getEmail());
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
