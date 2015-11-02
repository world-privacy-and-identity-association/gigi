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

    public DomainOverview(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CertificateOwner u = LoginPage.getAuthorizationContext(req).getTarget();
        String pi = req.getPathInfo();
        if (pi.length() - PATH.length() > 0) {
            int i = Integer.parseInt(pi.substring(PATH.length()));
            Domain d;
            try {
                d = Domain.getById(i);
            } catch (IllegalArgumentException e) {
                resp.getWriter().println(getLanguage(req).getTranslation("Access denied"));
                return;
            }
            if (u.getId() != d.getOwner().getId()) {
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
            DomainManagementForm domMan = new DomainManagementForm(req, u);
            HashMap<String, Object> vars = new HashMap<>();
            vars.put("doms", u.getDomains());
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
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pi = req.getPathInfo();
        if (pi.length() - PATH.length() > 0) {
            try {
                if (req.getParameter("configId") != null) {
                    if ( !Form.getForm(req, DomainPinglogForm.class).submit(resp.getWriter(), req)) {
                        // error?
                    }

                } else {
                    if ( !Form.getForm(req, PingConfigForm.class).submit(resp.getWriter(), req)) {

                    }
                }
            } catch (GigiApiException e) {
                e.format(resp.getWriter(), getLanguage(req));
                return;
            }

            resp.sendRedirect(pi);
        }
        if (req.getParameter("adddomain") != null) {
            DomainAddForm f = Form.getForm(req, DomainAddForm.class);
            if (f.submit(resp.getWriter(), req)) {
                resp.sendRedirect(PATH);
            }
        } else if (req.getParameter("domdel") != null) {
            DomainManagementForm f = Form.getForm(req, DomainManagementForm.class);
            if (f.submit(resp.getWriter(), req)) {
                resp.sendRedirect(PATH);
            }
        }
        super.doPost(req, resp);
    }
}
