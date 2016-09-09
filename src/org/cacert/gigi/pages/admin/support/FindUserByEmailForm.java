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

    public static class FindEmailResult extends SuccessMessageResult {

        private final EmailAddress[] emails;

        public FindEmailResult(EmailAddress[] emails) {
            super(null);
            this.emails = emails;
        }

        public EmailAddress[] getEmails() {
            return emails;
        }
    }

    private static final Template t = new Template(FindUserByDomainForm.class.getResource("FindUserByEmailForm.templ"));

    public FindUserByEmailForm(HttpServletRequest hsr) {
        super(hsr);
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        EmailAddress[] emails = EmailAddress.findByAllEmail(req.getParameter("email"));
        if (emails.length == 0) {
            throw new GigiApiException(SprintfCommand.createSimple("No users found matching {0}", req.getParameter("email")));
        }
        return new FindUserByEmailForm.FindEmailResult(emails);
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        t.output(out, l, vars);
    }
}
