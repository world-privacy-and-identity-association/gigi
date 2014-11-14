package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class ChangeForm extends Form {

    private User target;

    public ChangeForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    private static Template t;
    static {
        t = new Template(ChangePasswordPage.class.getResource("ChangePasswordForm.templ"));
    }

    @Override
    public void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        String oldpassword = req.getParameter("oldpassword");
        String p1 = req.getParameter("pword1");
        String p2 = req.getParameter("pword2");
        GigiApiException error = new GigiApiException();
        if (oldpassword == null || p1 == null || p2 == null) {
            new GigiApiException("All fields are required.").format(out, Page.getLanguage(req));
            return false;
        }
        if ( !p1.equals(p2)) {
            new GigiApiException("New passwords do not match.").format(out, Page.getLanguage(req));
            return false;
        }
        try {
            target.changePassword(oldpassword, p1);
        } catch (GigiApiException e) {
            error.mergeInto(e);
        }
        if ( !error.isEmpty()) {
            error.format(out, Page.getLanguage(req));
            return false;
        }
        return true;
    }

}
