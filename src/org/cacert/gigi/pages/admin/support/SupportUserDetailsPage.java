package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.SupportedUser;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.pages.Page;

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
        String ticket = (String) req.getSession().getAttribute("ticketNo" + user.getId());
        SupportUserDetailsForm f = new SupportUserDetailsForm(req, new SupportedUser(user, getUser(req), ticket));
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("details", f);
        vars.put("ticketNo", ticket);
        final EmailAddress[] addrs = user.getEmails();
        vars.put("emails", new IterableDataset() {

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (i == addrs.length) {
                    return false;
                }
                String address = addrs[i].getAddress();
                i++;
                if ( !address.equals(user.getEmail())) {
                    vars.put("secmail", address);
                }
                return true;
            }
        });
        vars.put("certifrevoke", new SupportRevokeCertificatesForm(req, new SupportedUser(user, getUser(req), ticket)));
        vars.put("tickethandling", new SupportEnterTicketForm(req, user));
        getDefaultTemplate().output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if (req.getParameter("setTicket") != null) {

                if ( !Form.getForm(req, SupportEnterTicketForm.class).submit(resp.getWriter(), req)) {
                    throw new GigiApiException("Invalid ticket number!");
                }
            } else if (req.getParameter("revokeall") != null) {
                if ( !Form.getForm(req, SupportRevokeCertificatesForm.class).submit(resp.getWriter(), req)) {
                    throw new GigiApiException("No ticket number set.");
                }
            } else if (req.getParameter("detailupdate") != null) {
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
    public boolean isPermitted(User u) {
        if (u == null) {
            return false;
        }
        return u.isInGroup(Group.SUPPORTER);
    }
}
