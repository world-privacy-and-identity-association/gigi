package club.wpia.gigi.pages.main;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.pages.ManagedFormPage;
import club.wpia.gigi.util.AuthorizationContext;

public class CertStatusRequestPage extends ManagedFormPage {

    public static final String PATH = "/certstatus";

    public CertStatusRequestPage() {
        super("Certificate Status", CertStatusRequestForm.class);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return true;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new CertStatusRequestForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

}
