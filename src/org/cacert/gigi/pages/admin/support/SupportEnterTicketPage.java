package org.cacert.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.pages.Page;

public class SupportEnterTicketPage extends Page {

    public static final String PATH = "/support/ticket";

    public SupportEnterTicketPage() {
        super("Set Ticket");
    }

    @Override
    public boolean beforeTemplate(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("setTicket") == null && req.getParameter("deleteTicket") == null) {
            return false;
        }
        SupportEnterTicketForm f = Form.getForm(req, SupportEnterTicketForm.class);
        try {
            if (f.submit(resp.getWriter(), req)) {
                resp.sendRedirect(PATH);
                return true;
            }
        } catch (GigiApiException e) {
            e.format(resp.getWriter(), getLanguage(req));
        }
        return false;

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("ticketNo", LoginPage.getAuthorizationContext(req).getSupporterTicketId());
        new SupportEnterTicketForm(req).output(resp.getWriter(), getLanguage(req), vars);
    }

}
