package club.wpia.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.dbObjects.Name;
import club.wpia.gigi.dbObjects.SupportedUser;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.ArrayIterable;
import club.wpia.gigi.output.DateSelector;
import club.wpia.gigi.output.GroupList;
import club.wpia.gigi.output.GroupSelector;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.account.MyDetails;
import club.wpia.gigi.util.AuthorizationContext;

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
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        if (user.getTicket() == null) {
            throw new GigiApiException("No ticket number set.");
        }

        if ((req.getParameter("detailupdate") != null ? 1 : 0) + (req.getParameter("addGroup") != null ? 1 : 0) + (req.getParameter("removeGroup") != null ? 1 : 0) + (req.getParameter("resetPass") != null ? 1 : 0) != 1) {
            throw new GigiApiException("More than one action requested!");
        }

        if (user.getTargetUser() == LoginPage.getUser(req)) {
            if (req.getParameter("removeGroup") != null) {
                value.update(req);
                Group toMod = value.getGroup();
                if (toMod == Group.SUPPORTER) {
                    user.revoke(toMod);
                    AuthorizationContext ac = LoginPage.getAuthorizationContext(req);
                    req.getSession().setAttribute(Gigi.AUTH_CONTEXT, new AuthorizationContext(ac.getActor(), ac.getActor()));
                    return new RedirectResult(MyDetails.PATH);
                }
            }
            throw new GigiApiException("Supporter may not modify himself.");
        }

        if (req.getParameter("addGroup") != null || req.getParameter("removeGroup") != null) {
            value.update(req);
            Group toMod = value.getGroup();
            if (req.getParameter("addGroup") != null) {
                user.grant(toMod);
            } else {
                user.revoke(toMod);
            }
            return new RedirectResult(req.getPathInfo());
        }
        if (req.getParameter("resetPass") != null) {
            String aword = req.getParameter("aword");
            if (aword == null || aword.equals("")) {
                throw new GigiApiException("An A-Word is required to perform a password reset.");
            }
            user.triggerPasswordReset(aword, req);
            return new SuccessMessageResult(new TranslateCommand("Password reset successful."));
        }
        dobSelector.update(req);
        if ( !dobSelector.isValid()) {
            throw new GigiApiException("Invalid date of birth!");
        }
        user.setDob(dobSelector.getDate());
        return new RedirectResult(req.getPathInfo());
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
                vars.put("preferred", t.getOwner().getPreferredName() == t);
                vars.put("points", Integer.toString(t.getVerificationPoints()));
            }

        });
        vars.put("agent", user.canVerify());
        vars.put("dob", dobSelector);
        vars.put("verificationPoints", user.getVerificationPoints());
        vars.put("exppoints", user.getExperiencePoints());
        final Set<Group> gr = user.getGroups();
        vars.put("support-groups", new GroupList(gr, true));
        vars.put("groups", new GroupList(gr, false));
        vars.put("groupSelector", value);
        t.output(out, l, vars);
    }

}
