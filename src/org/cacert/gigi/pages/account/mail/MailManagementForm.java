package org.cacert.gigi.pages.account.mail;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.TimeConditions;

public class MailManagementForm extends Form {

    private static final Template t = new Template(MailAddForm.class.getResource("MailManagementForm.templ"));

    private User target;

    public MailManagementForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        try {
            String d;
            if ((d = req.getParameter("default")) != null) {
                target.updateDefaultEmail(EmailAddress.getById(Integer.parseInt(d)));
            } else if ((d = req.getParameter("delete")) != null) {
                target.deleteEmail(EmailAddress.getById(Integer.parseInt(d)));
            } else if ((d = req.getParameter("reping")) != null) {
                EmailAddress.getById(Integer.parseInt(d)).requestReping(Page.getLanguage(req));
            }
            return new RedirectResult(MailOverview.DEFAULT_PATH);
        } catch (IOException e1) {
            throw new GigiApiException("Error while doing reping.");
        }
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        final EmailAddress[] emails = target.getEmails();
        IterableDataset ds = new IterableDataset() {

            private int point = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (point >= emails.length) {
                    return false;
                }
                EmailAddress emailAddress = emails[point];
                int mailID = emailAddress.getId();
                vars.put("id", mailID);
                if (emailAddress.getAddress().equals(target.getEmail())) {
                    vars.put("default", " disabled");
                    vars.put("deletable", " disabled");
                } else {
                    vars.put("deletable", "");
                    vars.put("default", "");
                }
                if (emailAddress.isVerified()) {
                    vars.put("verification", l.getTranslation("Verified"));
                } else {
                    // only verified emails may become the default email
                    // address.
                    vars.put("default", " disabled");
                    vars.put("verification", l.getTranslation("Unverified"));
                }
                vars.put("last_verification", emailAddress.getLastPing(true));
                if (target.getEmail().equals(emailAddress.getAddress())) {
                    vars.put("delete", "N/A");
                } else {
                    vars.put("delete", "<input type=\"checkbox\" name=\"delid[]\" value=\"" + mailID + "\"/>");
                }
                vars.put("address", emailAddress.getAddress());
                point++;
                return true;
            }

        };
        vars.put("emails", ds);
        vars.put("maxMonth", TimeConditions.getInstance().getEmailPingMonths());
        t.output(out, l, vars);
    }
}
