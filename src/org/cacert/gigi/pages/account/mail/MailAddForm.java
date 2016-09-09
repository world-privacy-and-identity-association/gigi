package org.cacert.gigi.pages.account.mail;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.PlainOutputable;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class MailAddForm extends Form {

    private static final Template t = new Template(MailAddForm.class.getResource("MailAddForm.templ"));;

    private String mail;

    private User target;

    public MailAddForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        String formMail = req.getParameter("newemail");
        mail = formMail;
        try {
            new EmailAddress(target, mail, Page.getLanguage(req).getLocale());
        } catch (IllegalArgumentException e) {
            throw new GigiApiException(new PlainOutputable("Invalid address."));
        }
        return new RedirectResult(MailOverview.DEFAULT_PATH);
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

}
