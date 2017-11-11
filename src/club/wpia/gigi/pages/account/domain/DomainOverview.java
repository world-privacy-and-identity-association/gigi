package club.wpia.gigi.pages.account.domain;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Form.CSRFException;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.ManagedMultiFormPage;

public class DomainOverview extends ManagedMultiFormPage {

    public static final String PATH = "/account/domains";

    public DomainOverview() {
        super("Domains");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CertificateOwner u = LoginPage.getAuthorizationContext(req).getTarget();
        try {
            DomainManagementForm domMan = new DomainManagementForm(req, u, false);
            Map<String, Object> vars = getDefaultVars(req);
            vars.put("domainman", domMan);
            if (u instanceof User) {
                DomainAddForm domAdd = new DomainAddForm(req, (User) u);
                vars.put("domainadd", domAdd);
            }
            getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req), getDefaultVars(req));
        }
    }

    @Override
    public Form getForm(HttpServletRequest req) throws CSRFException {
        if (req.getParameter("adddomain") != null) {
            return Form.getForm(req, DomainAddForm.class);
        } else if (req.getParameter("delete") != null) {
            return Form.getForm(req, DomainManagementForm.class);
        }
        throw new CSRFException();
    }
}
