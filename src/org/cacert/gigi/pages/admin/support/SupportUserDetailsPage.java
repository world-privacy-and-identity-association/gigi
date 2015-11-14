package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.SupportedUser;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.AuthorizationContext;

public class SupportUserDetailsPage extends Page {

    public static final String PATH = "/support/user/";

    public SupportUserDetailsPage(String title) {
        super(title);
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = -1;
        String[] idP = req.getPathInfo().split("/");
        try {
            id = Integer.parseInt(idP[idP.length - 1]);
        } catch (NumberFormatException e) {
            resp.sendError(404);
        }
        final User user = User.getById(id);
        SupportedUser targetUser = new SupportedUser(user, getUser(req), LoginPage.getAuthorizationContext(req).getSupporterTicketId());
        SupportUserDetailsForm f = new SupportUserDetailsForm(req, targetUser);
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("details", f);
        final EmailAddress[] addrs = user.getEmails();
        vars.put("emails", new IterableDataset() {

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                for (; i < addrs.length;) {
                    String address = addrs[i++].getAddress();
                    if ( !address.equals(user.getEmail())) {
                        vars.put("secmail", address);
                        return true;
                    }
                }
                return false;
            }
        });
        vars.put("certifrevoke", new SupportRevokeCertificatesForm(req, targetUser));
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (req.getParameter("revokeall") != null) {
                if ( !Form.getForm(req, SupportRevokeCertificatesForm.class).submit(resp.getWriter(), req)) {
                    throw new GigiApiException("No ticket number set.");
                }
            } else if (req.getParameter("detailupdate") != null || req.getParameter("resetPass") != null) {
                if ( !Form.getForm(req, SupportUserDetailsForm.class).submit(resp.getWriter(), req)) {
                    throw new GigiApiException("No ticket number set.");
                }
            }
        } catch (GigiApiException e) {
            e.printStackTrace();
            e.format(resp.getWriter(), getLanguage(req));
        }
        super.doPost(req, resp);
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canSupport();
    }
}
