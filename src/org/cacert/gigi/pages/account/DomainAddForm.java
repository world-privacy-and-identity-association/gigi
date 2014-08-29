package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Domain;
import org.cacert.gigi.Gigi;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.User;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.OutputableArrayIterable;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.Page;
import org.cacert.gigi.ping.SSLPinger;
import org.cacert.gigi.util.RandomToken;

public class DomainAddForm extends Form {

    private static final Template t = new Template(DomainManagementForm.class.getResource("DomainAddForm.templ"));

    private User target;

    private String tokenName = RandomToken.generateToken(8);

    private String tokenValue = RandomToken.generateToken(16);

    private static final int MAX_SSL_TESTS = 4;

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
            if (req.getParameter("emailType") != null) {
                String mail = AUTHORATIVE_EMAILS[Integer.parseInt(req.getParameter("email"))];
                d.addPing("email", mail);
            }
            if (req.getParameter("DNSType") != null) {
                d.addPing("dns", tokenName + ":" + tokenValue);
            }
            if (req.getParameter("HTTPType") != null) {
                d.addPing("http", tokenName + ":" + tokenValue);
            }
            if (req.getParameter("SSLType") != null) {
                List<String> types = Arrays.asList(SSLPinger.TYPES);
                for (int i = 0; i < MAX_SSL_TESTS; i++) {
                    String type = req.getParameter("ssl-type-" + i);
                    String port = req.getParameter("ssl-port-" + i);
                    if (type == null || port == null || port.equals("")) {
                        continue;
                    }
                    int portInt = Integer.parseInt(port);
                    if ("direct".equals(type)) {
                        d.addPing("ssl", port);
                    } else if (types.contains(type)) {
                        d.addPing("ssl", portInt + ":" + type);
                    }

                }
            }
            Gigi.notifyPinger();

            return true;
        } catch (NumberFormatException e) {
            new GigiApiException("A number could not be parsed").format(out, Page.getLanguage(req));
            return false;
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
                if (counter >= MAX_SSL_TESTS) {
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
