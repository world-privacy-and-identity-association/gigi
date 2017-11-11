package club.wpia.gigi.pages.account.domain;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.CertificateOwner;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Form.CSRFException;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.ManagedMultiFormPage;

public class EditDomain extends ManagedMultiFormPage {

    public static final String PATH = "/account/domains/";

    public EditDomain() {
        super("Domain");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CertificateOwner u = LoginPage.getAuthorizationContext(req).getTarget();
        String pi = req.getPathInfo();
        if (pi.length() - PATH.length() <= 0) {
            return;
        }
        Form.printFormErrors(req, resp.getWriter());
        int i = Integer.parseInt(pi.substring(PATH.length()));
        Domain d;
        try {
            d = Domain.getById(i);
        } catch (IllegalArgumentException e) {
            resp.getWriter().println(getLanguage(req).getTranslation("Access denied"));
            return;
        }
        if (d == null || u.getId() != d.getOwner().getId()) {
            resp.getWriter().println(getLanguage(req).getTranslation("Access denied"));
            return;
        }
        new DomainPinglogForm(req, d).output(resp.getWriter(), getLanguage(req), getDefaultVars(req));
        try {
            new PingConfigForm(req, d).output(resp.getWriter(), getLanguage(req), getDefaultVars(req));
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req), getDefaultVars(req));
        }

    }

    @Override
    public Form getForm(HttpServletRequest req) throws CSRFException {
        String pi = req.getPathInfo();
        if (pi.length() - PATH.length() <= 0) {
            return null;
        }
        if (req.getParameter("configId") != null) {
            return Form.getForm(req, DomainPinglogForm.class);
        } else {
            return Form.getForm(req, PingConfigForm.class);
        }
    }

}
