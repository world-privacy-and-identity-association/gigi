package club.wpia.gigi.pages.account.mail;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.EmailAddress;
import club.wpia.gigi.dbObjects.User;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.PlainOutputable;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.pages.Page;

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
