package club.wpia.gigi.pages.account.certs;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.pages.ManagedFormPage;
import club.wpia.gigi.util.AuthorizationContext;

public class CertificateAdd extends ManagedFormPage {

    public static final String PATH = "/account/certs/new";

    public CertificateAdd() {
        super("Create certificate", CertificateIssueForm.class);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new CertificateIssueForm(req).output(resp.getWriter(), getLanguage(req), getDefaultVars(req));
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return super.isPermitted(ac) && !ac.isInGroup(Group.BLOCKEDCERT);
    }
}
