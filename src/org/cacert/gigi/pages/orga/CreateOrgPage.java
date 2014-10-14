package org.cacert.gigi.pages.orga;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.pages.Page;

public class CreateOrgPage extends Page {

    public static final Group ORG_ASSURER = Group.getByString("orgassurer");

    public static final String DEFAULT_PATH = "/orga/new";

    public CreateOrgPage() {
        super("Create Organisation");
    }

    @Override
    public boolean isPermitted(User u) {
        return u != null && u.isInGroup(ORG_ASSURER);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            CreateOrgForm form = Form.getForm(req, CreateOrgForm.class);
            if (form.submit(resp.getWriter(), req)) {
                resp.sendRedirect(ViewOrgPage.DEFAULT_PATH + "/" + form.getResult().getId());
                return;
            }
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new CreateOrgForm(req).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }
}
