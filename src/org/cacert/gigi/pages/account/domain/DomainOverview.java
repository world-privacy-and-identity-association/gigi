package org.cacert.gigi.pages.account.domain;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Form.CSRFException;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.ManagedMultiFormPage;

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
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("domainman", domMan);
            if (u instanceof User) {
                DomainAddForm domAdd = new DomainAddForm(req, (User) u);
                vars.put("domainadd", domAdd);
            }
            getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
    }

    @Override
    public Form getForm(HttpServletRequest req) throws CSRFException {
        if (req.getParameter("adddomain") != null) {
            return Form.getForm(req, DomainAddForm.class);
        } else if (req.getParameter("delete") != null) {
            return Form.getForm(req, DomainManagementForm.class);
        }
        return null;
    }
}
