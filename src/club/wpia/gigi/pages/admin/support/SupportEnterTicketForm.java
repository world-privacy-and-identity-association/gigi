package club.wpia.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.LoginPage;
import club.wpia.gigi.util.AuthorizationContext;

public class SupportEnterTicketForm extends Form {

    private static final Template t = new Template(SupportEnterTicketForm.class.getResource("SupportEnterTicketForm.templ"));

    public SupportEnterTicketForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        if (req.getParameter("setTicket") != null) {
            // [asdmASDM]\d{8}\.\d+
            String ticket = req.getParameter("ticketno");
            if (ticket.matches("[asdmASDM]\\d{8}\\.\\d+")) {
                AuthorizationContext ac = LoginPage.getAuthorizationContext(req);
                req.getSession().setAttribute(Gigi.AUTH_CONTEXT, new AuthorizationContext(ac.getActor(), ticket));
                return new RedirectResult(SupportEnterTicketPage.PATH);
            }
            throw new GigiApiException("Ticket format malformed");
        } else if (req.getParameter("deleteTicket") != null) {
            AuthorizationContext ac = LoginPage.getAuthorizationContext(req);
            req.getSession().setAttribute(Gigi.AUTH_CONTEXT, new AuthorizationContext(ac.getActor(), ac.getActor()));
            return new RedirectResult(SupportEnterTicketPage.PATH);
        }
        throw new GigiApiException("No valid action given.");
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

}
