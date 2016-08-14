package org.cacert.gigi.pages.orga;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class CreateOrgPage extends Page {

    public static final Group ORG_ASSURER = Group.getByString("orgassurer");

    public static final String DEFAULT_PATH = "/orga/new";

    public CreateOrgPage() {
        super("Create Organisation");
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.isInGroup(ORG_ASSURER);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CreateOrgForm form = Form.getForm(req, CreateOrgForm.class);
        if (form.submitProtected(resp.getWriter(), req)) {
            resp.sendRedirect(ViewOrgPage.DEFAULT_PATH + "/" + form.getResult().getId());
            return;
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new CreateOrgForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }
}
