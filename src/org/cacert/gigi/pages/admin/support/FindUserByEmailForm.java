package org.cacert.gigi.pages.admin.support;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.SprintfCommand;
import org.cacert.gigi.output.template.Template;

public class FindUserByEmailForm extends Form {

    private EmailAddress emails[];

    private static final Template t = new Template(FindUserByDomainForm.class.getResource("FindUserByEmailForm.templ"));

    public FindUserByEmailForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public boolean submit(HttpServletRequest req) throws GigiApiException {
        EmailAddress[] emails = EmailAddress.findByAllEmail(req.getParameter("email"));
        if (emails.length == 0) {
            throw new GigiApiException(SprintfCommand.createSimple("No users found matching {0}", req.getParameter("email")));
        }
        this.emails = emails;
        return true;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }

    public EmailAddress[] getEmails() {
        return emails;
    }

}
