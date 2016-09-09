package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.SupportedUser;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Form.CSRFException;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.ManagedMultiFormPage;
import org.cacert.gigi.util.AuthorizationContext;

public class SupportUserDetailsPage extends ManagedMultiFormPage {

    public static final String PATH = "/support/user/";

    public SupportUserDetailsPage() {
        super("Support: User Details");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getUser(req, resp);
        if (user == null) {
            return;
        }
        SupportedUser targetUser = new SupportedUser(user, getUser(req), LoginPage.getAuthorizationContext(req).getSupporterTicketId());
        outputContents(req, resp, user, new SupportRevokeCertificatesForm(req, targetUser), new SupportUserDetailsForm(req, targetUser));
    }

    private User getUser(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int id = -1;
        if ( !req.getPathInfo().endsWith("/")) {
            resp.sendError(404);
            return null;
        }
        String[] idP = req.getPathInfo().split("/");
        try {
            id = Integer.parseInt(idP[idP.length - 1]);
        } catch (NumberFormatException e) {
            resp.sendError(404);
            return null;
        }
        final User user = User.getById(id);
        return user;
    }

    private void outputContents(HttpServletRequest req, HttpServletResponse resp, final User user, SupportRevokeCertificatesForm certificatesForm, SupportUserDetailsForm f) throws IOException {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("details", f);
        final EmailAddress[] addrs = user.getEmails();
        vars.put("emails", new IterableDataset() {

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                for (; i < addrs.length;) {
                    EmailAddress secAddress = addrs[i++];
                    String address = secAddress.getAddress();
                    if ( !address.equals(user.getEmail())) {
                        vars.put("secmail", address);
                        vars.put("status", l.getTranslation(secAddress.isVerified() ? "verified" : "not verified"));
                        return true;
                    }
                }
                return false;
            }
        });

        final Domain[] doms = user.getDomains();
        vars.put("domains", new IterableDataset() {

            private int point = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (point >= doms.length) {
                    return false;
                }
                Domain domain = doms[point];
                vars.put("domain", domain.getSuffix());
                vars.put("status", l.getTranslation(domain.isVerified() ? "verified" : "not verified"));
                point++;
                return true;
            }
        });

        vars.put("certifrevoke", certificatesForm);
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = getUser(req, resp);
        if (user == null) {
            return;
        }
        if (Form.printFormErrors(req, resp.getWriter())) {
            Form f = getForm(req);
            SupportedUser targetUser = new SupportedUser(user, getUser(req), LoginPage.getAuthorizationContext(req).getSupporterTicketId());

            if (f instanceof SupportUserDetailsForm) {
                outputContents(req, resp, user, new SupportRevokeCertificatesForm(req, targetUser), (SupportUserDetailsForm) f);
            } else if (f instanceof SupportRevokeCertificatesForm) {
                outputContents(req, resp, user, (SupportRevokeCertificatesForm) f, new SupportUserDetailsForm(req, targetUser));
            }
        }

    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.canSupport();
    }

    @Override
    public Form getForm(HttpServletRequest req) throws CSRFException {
        if (req.getParameter("revokeall") != null) {
            return Form.getForm(req, SupportRevokeCertificatesForm.class);
        } else if (req.getParameter("detailupdate") != null || req.getParameter("resetPass") != null || req.getParameter("removeGroup") != null || req.getParameter("addGroup") != null) {
            return Form.getForm(req, SupportUserDetailsForm.class);
        }
        return null;
    }
}
