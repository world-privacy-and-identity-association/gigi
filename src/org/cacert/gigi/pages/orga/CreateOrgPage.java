package org.cacert.gigi.pages.orga;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.pages.ManagedFormPage;
import org.cacert.gigi.util.AuthorizationContext;

public class CreateOrgPage extends ManagedFormPage {

    public static final Group ORG_ASSURER = Group.ORGASSURER;

    public static final String DEFAULT_PATH = "/orga/new";

    public CreateOrgPage() {
        super("Create Organisation", CreateOrgForm.class);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.isInGroup(ORG_ASSURER);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new CreateOrgForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }
}
