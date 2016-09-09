package org.cacert.gigi.pages.account.certs;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.pages.ManagedFormPage;
import org.cacert.gigi.util.AuthorizationContext;

public class CertificateAdd extends ManagedFormPage {

    public static final String PATH = "/account/certs/new";

    public CertificateAdd() {
        super("Create certificate", CertificateIssueForm.class);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new CertificateIssueForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return super.isPermitted(ac) && !ac.isInGroup(Group.BLOCKEDCERT);
    }
}
