package org.cacert.gigi.pages.account;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingConfiguration;
import org.cacert.gigi.dbObjects.DomainPingConfiguration.PingType;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.Form;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.ping.SSLPinger;
import org.cacert.gigi.util.RandomToken;

public class PingconfigForm extends Form {

    public enum SSLType {
        DIRECT, XMPP, XMPP_SERVER, SMTP, IMAP;

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    private Domain target;

    private String tokenName = RandomToken.generateToken(8);

    private String tokenValue = RandomToken.generateToken(16);

    private static final int MAX_SSL_TESTS = 4;

    public static final String[] AUTHORATIVE_EMAILS = new String[] {
            "root", "hostmaster", "postmaster", "admin", "webmaster"
    };

    private int selectedMail = -1;

    private boolean doMail, doDNS, doHTTP, doSSL;

    private int[] ports = new int[MAX_SSL_TESTS];

    private SSLType[] sslTypes = new SSLType[MAX_SSL_TESTS];

    private final Template t = new Template(PingconfigForm.class.getResource("PingconfigForm.templ"));

    public PingconfigForm(HttpServletRequest hsr, Domain target) throws GigiApiException {
        super(hsr);
        this.target = target;
        if (target == null) {
            return;
        }
        List<DomainPingConfiguration> configs = target.getConfiguredPings();
        int portpos = 0;
        for (DomainPingConfiguration dpc : configs) {
            switch (dpc.getType()) {
            case EMAIL:
                doMail = true;
                for (int i = 0; i < AUTHORATIVE_EMAILS.length; i++) {
                    if (AUTHORATIVE_EMAILS[i].equals(dpc.getInfo())) {
                        selectedMail = i;
                    }
                }
                break;
            case DNS: {
                doDNS = true;
                String[] parts = dpc.getInfo().split(":");
                tokenName = parts[0];
                tokenValue = parts[1];
                break;
            }
            case HTTP: {
                doHTTP = true;
                String[] parts = dpc.getInfo().split(":");
                tokenName = parts[0];
                tokenValue = parts[1];
                break;
            }
            case SSL: {
                doSSL = true;
                String[] parts = dpc.getInfo().split(":");
                ports[portpos] = Integer.parseInt(parts[0]);
                if (parts.length == 2) {
                    sslTypes[portpos] = SSLType.valueOf(parts[1].toUpperCase());
                } else {
                    sslTypes[portpos] = SSLType.DIRECT;
                }
                portpos++;
                break;
            }
            }
        }
    }

    public void setTarget(Domain target) {
        this.target = target;
    }

    @Override
    public boolean submit(PrintWriter out, HttpServletRequest req) throws GigiApiException {
        if (req.getParameter("emailType") != null) {
            String mail = AUTHORATIVE_EMAILS[Integer.parseInt(req.getParameter("email"))];
            target.addPing(PingType.EMAIL, mail);
        }
        if (req.getParameter("DNSType") != null) {
            target.addPing(PingType.DNS, tokenName + ":" + tokenValue);
        }
        if (req.getParameter("HTTPType") != null) {
            target.addPing(PingType.HTTP, tokenName + ":" + tokenValue);
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
                    target.addPing(PingType.SSL, port);
                } else if (types.contains(type)) {
                    target.addPing(PingType.SSL, portInt + ":" + type);
                }

            }
        }
        Gigi.notifyPinger();
        return false;
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        out.print("<table class=\"wrapper dataTable\"><tbody>");
        outputEmbeddableContent(out, l, vars);
        out.print("<tr><td></td><td><input type=\"submit\" value=\"Update\"/></td></tbody></table>");
    }

    protected void outputEmbeddableContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("tokenName", tokenName);
        vars.put("tokenValue", tokenValue);
        vars.put("authEmails", new IterableDataset() {

            int i = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (i >= AUTHORATIVE_EMAILS.length) {
                    return false;
                }
                vars.put("i", i);
                vars.put("email", AUTHORATIVE_EMAILS[i]);
                if (i == selectedMail) {
                    vars.put("checked", " checked=\"checked\"");
                } else {
                    vars.put("checked", "");
                }

                i++;
                return true;
            }
        });
        vars.put("mail", doMail ? " checked=\"checked\"" : "");
        vars.put("dns", doDNS ? " checked=\"checked\"" : "");
        vars.put("http", doHTTP ? " checked=\"checked\"" : "");
        vars.put("ssl", doSSL ? " checked=\"checked\"" : "");
        vars.put("ssl-services", new IterableDataset() {

            int counter = 0;

            @Override
            public boolean next(Language l, Map<String, Object> vars) {
                if (counter >= MAX_SSL_TESTS) {
                    return false;
                }
                vars.put("i", counter);
                vars.put("port", ports[counter] == 0 ? "" : Integer.toString(ports[counter]));
                final SSLType selectedType = sslTypes[counter];
                vars.put("ssl-types", new IterableDataset() {

                    int i = 0;

                    SSLType[] type = SSLType.values();

                    @Override
                    public boolean next(Language l, Map<String, Object> vars) {
                        if (i >= type.length) {
                            return false;
                        }
                        vars.put("name", type[i].toString());
                        if (selectedType == type[i]) {
                            vars.put("selected", " selected=\"selected\"");
                        } else {
                            vars.put("selected", "");
                        }
                        i++;
                        return true;
                    }
                });
                counter++;
                return true;
            }
        });
        t.output(out, l, vars);
    }
}
