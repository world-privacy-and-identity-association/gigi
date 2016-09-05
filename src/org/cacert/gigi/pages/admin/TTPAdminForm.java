package org.cacert.gigi.pages.admin;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;

public class TTPAdminForm extends Form {

    private static final Template t = new Template(TTPAdminForm.class.getResource("TTPAdminForm.templ"));

    User u;

    User ttpAdmin;

    public TTPAdminForm(HttpServletRequest hsr, User u) {
        super(hsr);
        this.u = u;
        ttpAdmin = LoginPage.getUser(hsr);
    }

    @Override
    public boolean submit(HttpServletRequest req) throws GigiApiException {
        if (req.getParameter("deny") != null) {
            u.revokeGroup(ttpAdmin, TTPAdminPage.TTP_APPLICANT);
        }
        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("name", u.getPreferredName());
        vars.put("email", u.getEmail());
        vars.put("DoB", u.getDoB());
        vars.put("uid", Integer.toString(u.getId()));
        t.output(out, l, vars);
    }
}
