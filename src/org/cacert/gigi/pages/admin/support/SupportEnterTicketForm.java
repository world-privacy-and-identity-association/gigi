package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.Template;

public class SupportEnterTicketForm extends Form {

    private static Template t;

    private User target;
    static {
        t = new Template(SupportEnterTicketForm.class.getResource("SupportEnterTicketForm.templ"));
    }

    public SupportEnterTicketForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        // [asdmASDM]\d{8}\.\d+
        String ticket = req.getParameter("ticketno");
        if (ticket.matches("[asdmASDM]\\d{8}\\.\\d+")) {
            req.getSession().setAttribute("ticketNo" + target.getId(), ticket);
            return true;
        }
        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

}
