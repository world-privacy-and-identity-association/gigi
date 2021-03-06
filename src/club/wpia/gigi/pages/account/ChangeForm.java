package club.wpia.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.output.template.TranslateCommand;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.util.AuthorizationContext;

public class ChangeForm extends Form {

    private User target;

    private AuthorizationContext c;

    public ChangeForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
        c = LoginPage.getAuthorizationContext(hsr);
    }

    private static final Template t = new Template(ChangePasswordPage.class.getResource("ChangePasswordForm.templ"));

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        if ( !c.isStronglyAuthenticated()) {
            vars.put("currentLoginMethod", "pw");
        }
        t.output(out, l, vars);
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        String oldpassword = req.getParameter("oldpassword");
        String p1 = req.getParameter("pword1");
        String p2 = req.getParameter("pword2");
        GigiApiException error = new GigiApiException();
        if ((oldpassword == null && !c.isStronglyAuthenticated()) || p1 == null || p2 == null) {
            throw new GigiApiException("All fields are required.");
        }
        if ( !p1.equals(p2)) {
            throw new GigiApiException("New passwords do not match.");
        }
        try {
            if (c.isStronglyAuthenticated()) {
                target.setPassword(p1);
            } else {
                target.changePassword(oldpassword, p1);
            }
            target.writeUserLog(target, "User triggered password reset");
        } catch (GigiApiException e) {
            error.mergeInto(e);
        }
        if ( !error.isEmpty()) {
            throw error;
        }
        return new SuccessMessageResult(new TranslateCommand("Password changed."));
    }

}
