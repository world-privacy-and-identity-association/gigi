package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.EmailAddress;
import org.cacert.gigi.Language;
import org.cacert.gigi.User;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class MailAddForm extends Form {

    private static Template t;

    private String mail;
    static {
        t = new Template(ChangePasswordPage.class.getResource("MailAddForm.templ"));
    }

    User target;

    public MailAddForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        String formMail = req.getParameter("newemail");
        mail = formMail;
        try {
            EmailAddress addr = new EmailAddress(mail, target);
            addr.insert(Page.getLanguage(req));
        } catch (IllegalArgumentException e) {
            out.println("<div class='formError'>Error: Invalid address!</div>");
            return false;
        }
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

}
