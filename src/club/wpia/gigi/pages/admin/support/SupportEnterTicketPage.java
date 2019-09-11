package club.wpia.gigi.pages.admin.support;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import club.wpia.gigi.dbObjects.Group;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.pages.Page;
import club.wpia.gigi.util.AuthorizationContext;

public class SupportEnterTicketPage extends Page {

    public static final String PATH = "/support/ticket";

    public SupportEnterTicketPage() {
        super("Set Ticket");
    }

    @Override
    public boolean beforePost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("setTicket") == null && req.getParameter("deleteTicket") == null) {
            return false;
        }
        SupportEnterTicketForm f = Form.getForm(req, SupportEnterTicketForm.class);
        return f.submitExceptionProtected(req, resp);

    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("ticketNo", LoginPage.getAuthorizationContext(req).getSupporterTicketId());
        new SupportEnterTicketForm(req).output(resp.getWriter(), getLanguage(req), vars);
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (Form.printFormErrors(req, resp.getWriter())) {
            SupportEnterTicketForm f = Form.getForm(req, SupportEnterTicketForm.class);
            f.output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
        }
    }

    @Override
    public boolean isPermitted(AuthorizationContext ac) {
        return ac != null && ac.isInGroup(Group.SUPPORTER) && ac.isStronglyAuthenticated();
    }

}
