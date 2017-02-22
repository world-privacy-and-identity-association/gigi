package club.wpia.gigi.pages.orga;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.pages.ManagedFormPage;
import club.wpia.gigi.util.AuthorizationContext;

public class CreateOrgPage extends ManagedFormPage {

    public static final Group ORG_AGENT = Group.ORGASSURER;

    public static final String DEFAULT_PATH = "/orga/new";

    public CreateOrgPage() {
        super("Create Organisation", CreateOrgForm.class);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.isInGroup(ORG_AGENT);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new CreateOrgForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }
}
