package org.cacert.gigi.pages.wot;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class MyListingPage extends Page {

    public static final String PATH = "/wot/listing";

    public MyListingPage() {
        super("My Listing");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.getForm(req, MyListingForm.class).submit(resp.getWriter(), req)) {
            resp.sendRedirect(PATH);
            return;
        }
        super.doPost(req, resp);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new MyListingForm(req, getUser(req)).output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.getTarget() instanceof User;
    }

}
