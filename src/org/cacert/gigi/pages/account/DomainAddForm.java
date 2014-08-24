package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Domain;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.OutputableArrayIterable;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.util.RandomToken;

public class DomainAddForm extends Form {

    private static final Template t = new Template(DomainManagementForm.class.getResource("DomainAddForm.templ"));

    private User target;

    private String tokenName = RandomToken.generateToken(8);

    private String tokenValue = RandomToken.generateToken(16);

    public DomainAddForm(HttpServletRequest hsr, User target) {
        super(hsr);
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) {
        try {
            String parameter = req.getParameter("newdomain");
            if (parameter.trim().isEmpty()) {
                throw new GigiApiException("No domain inserted.");
            }
            Domain d = new Domain(target, parameter);
            d.insert();
            return true;
        } catch (GigiApiException e) {
            e.format(out, Page.getLanguage(req));
            return false;
        }
    }

    public static final String[] AUTHORATIVE_EMAILS = new String[] {
            "root", "hostmaster", "postmaster", "admin", "webmaster"
    };

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("tokenName", tokenName);
        vars.put("tokenValue", tokenValue);
        vars.put("authEmails", new OutputableArrayIterable(AUTHORATIVE_EMAILS, "email"));
        vars.put("ssl-services", new IterableDataset() {

            int counter = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (counter >= 4) {
                    return false;
                }
                vars.put("i", counter);
                counter++;
                return true;
            }
        });
        t.output(out, l, vars);
    }
}
