package org.cacert.gigi.pages.account.mail;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.Form;
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
        Map<String, String[]> map = req.getParameterMap();
        try {
            for (Entry<String, String[]> e : map.entrySet()) {
                String k = e.getKey();
                String[] p = k.split(":", 2);
                if (p[0].equals("default")) {
                    target.updateDefaultEmail(EmailAddress.getById(Integer.parseInt(p[1])));
                }
                if (p[0].equals("delete")) {
                    target.deleteEmail(EmailAddress.getById(Integer.parseInt(p[1])));
                }
                if (p[0].equals("reping")) {
                    EmailAddress.getById(Integer.parseInt(p[1])).requestReping(Page.getLanguage(req));
                }
            }
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
            return false;
        } catch (IOException e1) {
            new GigiApiException("Error while doing reping.").format(out, Page.getLanguage(req));
            return false;
        }
        return true;
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
                } else {
                    vars.put("default", "");
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
