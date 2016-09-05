package org.cacert.gigi.pages.account.certs;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class CertificateAdd extends Page {

    public static final String PATH = "/account/certs/new";

    public CertificateAdd() {
        super("Create certificate");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new CertificateIssueForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CertificateIssueForm f = Form.getForm(req, CertificateIssueForm.class);
        if (f.submitExceptionProtected(req)) {
            Certificate c = f.getResult();
            if (c.getStatus() != CertificateStatus.ISSUED) {
                resp.getWriter().println("Timeout while waiting for certificate.");
                return false;
            }
            String ser = c.getSerial();
            if (ser.isEmpty()) {
                resp.getWriter().println("Timeout while waiting for certificate.");
                return false;
            }
            resp.sendRedirect(Certificates.PATH + "/" + ser);
            return true;
        }
        return super.beforePost(req, resp);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            CertificateIssueForm f = Form.getForm(req, CertificateIssueForm.class);
            f.output(resp.getWriter(), getLanguage(req), Collections.<String, Object>emptyMap());
        }
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return super.isPermitted(ac) && !ac.isInGroup(Group.BLOCKEDCERT);
    }
}
