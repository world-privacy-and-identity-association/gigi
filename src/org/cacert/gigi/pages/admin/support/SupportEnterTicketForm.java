package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.LoginPage;
import org.cacert.gigi.util.AuthorizationContext;

public class SupportEnterTicketForm extends Form {

    private static final Template t = new Template(SupportEnterTicketForm.class.getResource("SupportEnterTicketForm.templ"));

    public SupportEnterTicketForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public boolean submit(HttpServletRequest req) throws GigiApiException {
        if (req.getParameter("setTicket") != null) {
            // [asdmASDM]\d{8}\.\d+
            String ticket = req.getParameter("ticketno");
            if (ticket.matches("[asdmASDM]\\d{8}\\.\\d+")) {
                AuthorizationContext ac = LoginPage.getAuthorizationContext(req);
                req.getSession().setAttribute(Gigi.AUTH_CONTEXT, new AuthorizationContext(ac.getActor(), ticket));
                return true;
            }
            return false;
        } else if (req.getParameter("deleteTicket") != null) {
            AuthorizationContext ac = LoginPage.getAuthorizationContext(req);
            req.getSession().setAttribute(Gigi.AUTH_CONTEXT, new AuthorizationContext(ac.getActor(), ac.getActor()));
            return true;
        }
        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

}
