package org.cacert.gigi.pages.account.domain;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Form.CSRFException;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.ManagedMultiFormPage;

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
        new DomainPinglogForm(req, d).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        try {
            new PingConfigForm(req, d).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
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
