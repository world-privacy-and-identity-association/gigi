package club.wpia.gigi.pages.admin;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.LoginPage;

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
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        if (req.getParameter("deny") != null) {
            u.revokeGroup(ttpAdmin, TTPAdminPage.TTP_APPLICANT);
        }
        return new RedirectResult(TTPAdminPage.PATH);
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
