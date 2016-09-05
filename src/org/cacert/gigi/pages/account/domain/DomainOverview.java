package org.cacert.gigi.pages.account.domain;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

public class DomainOverview extends Page {

    public static final String PATH = "/account/domains/";

    public DomainOverview() {
        super("Domains");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CertificateOwner u = LoginPage.getAuthorizationContext(req).getTarget();
        String pi = req.getPathInfo();
        if (pi.length() - PATH.length() > 0) {
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
            return;

        }
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
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pi = req.getPathInfo();
        if (pi.length() - PATH.length() > 0) {
            if (req.getParameter("configId") != null) {
                if (Form.getForm(req, DomainPinglogForm.class).submitExceptionProtected(req)) {
                    resp.sendRedirect(pi);
                    return true;
                }

            } else {
                if (Form.getForm(req, PingConfigForm.class).submitExceptionProtected(req)) {
                    resp.sendRedirect(pi);
                    return true;
                }
            }

        }
        return super.beforePost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("adddomain") != null) {
            DomainAddForm f = Form.getForm(req, DomainAddForm.class);
            if (f.submitProtected(resp.getWriter(), req)) {
                resp.sendRedirect(PATH);
            }
        } else if (req.getParameter("delete") != null) {
            DomainManagementForm f = Form.getForm(req, DomainManagementForm.class);
            if (f.submitProtected(resp.getWriter(), req)) {
                resp.sendRedirect(PATH);
            }
        }
        super.doPost(req, resp);
    }
}
