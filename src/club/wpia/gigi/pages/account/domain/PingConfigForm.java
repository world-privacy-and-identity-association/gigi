package club.wpia.gigi.pages.account.domain;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import club.wpia.gigi.Gigi;
import club.wpia.gigi.GigiApiException;
import club.wpia.gigi.dbObjects.Domain;
import club.wpia.gigi.dbObjects.DomainPingConfiguration;
import club.wpia.gigi.dbObjects.DomainPingType;
import club.wpia.gigi.localisation.Language;
import club.wpia.gigi.output.template.Form;
import club.wpia.gigi.output.template.IterableDataset;
import club.wpia.gigi.output.template.Template;
import club.wpia.gigi.ping.SSLPinger;
import club.wpia.gigi.util.RandomToken;
import club.wpia.gigi.util.SystemKeywords;

public class PingConfigForm extends Form {

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

    private static final Template t = new Template(PingConfigForm.class.getResource("PingConfigForm.templ"));

    public PingConfigForm(HttpServletRequest hsr, Domain target) throws GigiApiException {
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
                tokenName = parts[0];
                tokenValue = parts[1];
                ports[portpos] = Integer.parseInt(parts[2]);
                if (parts.length == 4) {
                    sslTypes[portpos] = SSLType.valueOf(parts[3].toUpperCase());
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
    public SubmissionResult submit(HttpServletRequest req) throws GigiApiException {
        target.clearPings();
        if (req.getParameter("emailType") != null && req.getParameter("email") != null) {
            try {
                String mail = AUTHORATIVE_EMAILS[Integer.parseInt(req.getParameter("email"))];
                target.addPing(DomainPingType.EMAIL, mail);
            } catch (NumberFormatException e) {
                throw new GigiApiException("A email address is required");
            }
        }
        if (req.getParameter("DNSType") != null) {
            target.addPing(DomainPingType.DNS, tokenName + ":" + tokenValue);
        }
        if (req.getParameter("HTTPType") != null) {
            target.addPing(DomainPingType.HTTP, tokenName + ":" + tokenValue);
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
                    target.addPing(DomainPingType.SSL, tokenName + ":" + tokenValue + ":" + port);
                } else if (types.contains(type)) {
                    target.addPing(DomainPingType.SSL, tokenName + ":" + tokenValue + ":" + portInt + ":" + type);
                }

            }
        }
        Gigi.notifyPinger(null);
        return new RedirectResult(req.getPathInfo());
    }

    @Override
    protected void outputContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("notEmbedded", true);
        outputEmbeddableContent(out, l, vars);
    }

    protected void outputEmbeddableContent(PrintWriter out, Language l, Map<String, Object> vars) {
        vars.put("httpPrefix", SystemKeywords.HTTP_CHALLENGE_PREFIX);
        vars.put("dnsPrefix", SystemKeywords.DNS_PREFIX);
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
