package org.cacert.gigi.pages.account.mail;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;

public class MailManagementForm extends Form {

    private static Template t;

    private User target;
    static {
        t = new Template(MailAddForm.class.getResource("MailManagementForm.templ"));
    }

    public MailManagementForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        if (req.getParameter("makedefault") != null) {
            try {
                String mailid = req.getParameter("emailid");
                if (mailid == null) {
                    return false;
                }
                target.updateDefaultEmail(EmailAddress.getById(Integer.parseInt(mailid.trim())));
            } catch (GigiApiException e) {
                e.format(out, Page.getLanguage(req));
                return false;
            }
            return true;
        }
        if (req.getParameter("delete") != null) {
            String[] toDel = req.getParameterValues("delid[]");
            if (toDel == null) {
                return false;
            }
            for (int i = 0; i < toDel.length; i++) {
                try {
                    target.deleteEmail(EmailAddress.getById(Integer.parseInt(toDel[i].trim())));
                } catch (GigiApiException e) {
                    e.format(out, Page.getLanguage(req));
                    return false;
                }
            }
            return true;

        }
        return false;
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
                    vars.put("checked", "checked");
                } else {
                    vars.put("checked", "");
                }
                if (emailAddress.isVerified()) {
                    vars.put("verification", "Verified");
                } else {
                    vars.put("verification", "Unverified");
                }
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
        t.output(out, l, vars);
    }
}
