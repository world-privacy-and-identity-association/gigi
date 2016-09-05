package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

public class ChangeForm extends Form {

    private User target;

    public ChangeForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    private static final Template t = new Template(ChangePasswordPage.class.getResource("ChangePasswordForm.templ"));

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

    @Override
    public boolean submit(HttpServletRequest req) throws GigiApiException {
        String oldpassword = req.getParameter("oldpassword");
        String p1 = req.getParameter("pword1");
        String p2 = req.getParameter("pword2");
        GigiApiException error = new GigiApiException();
        if (oldpassword == null || p1 == null || p2 == null) {
            throw new GigiApiException("All fields are required.");
        }
        if ( !p1.equals(p2)) {
            throw new GigiApiException("New passwords do not match.");
        }
        try {
            target.changePassword(oldpassword, p1);
        } catch (GigiApiException e) {
            error.mergeInto(e);
        }
        if ( !error.isEmpty()) {
            throw error;
        }
        return true;
    }

}
