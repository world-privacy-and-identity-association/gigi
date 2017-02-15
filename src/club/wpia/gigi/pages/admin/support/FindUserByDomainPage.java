package club.wpia.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.pages.ManagedFormPage;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;

public class FindUserByDomainPage extends ManagedFormPage {

    public static final String PATH = "/support/find/domain";

    public FindUserByDomainPage() {
        super("Find Domain", FindUserByDomainForm.class);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new FindUserByDomainForm(req).output(resp.getWriter(), Page.getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canSupport();
    }
}
