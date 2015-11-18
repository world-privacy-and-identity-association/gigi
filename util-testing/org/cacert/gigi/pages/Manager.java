package org.cacert.gigi.pages;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.sql.Date;
import java.util.Base64;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cacert.gigi.Gigi;
import org.cacert.gigi.GigiApiException;
import org.cacert.gigi.crypto.SPKAC;
import org.cacert.gigi.database.DatabaseConnection;
import org.cacert.gigi.database.GigiPreparedStatement;
import org.cacert.gigi.dbObjects.Certificate;
import org.cacert.gigi.dbObjects.Certificate.CertificateStatus;
import org.cacert.gigi.dbObjects.CertificateOwner;
import org.cacert.gigi.dbObjects.Digest;
import org.cacert.gigi.dbObjects.Domain;
import org.cacert.gigi.dbObjects.DomainPingType;
import org.cacert.gigi.dbObjects.EmailAddress;
import org.cacert.gigi.dbObjects.Group;
import org.cacert.gigi.dbObjects.Name;
import org.cacert.gigi.dbObjects.User;
import org.cacert.gigi.email.EmailProvider;
import org.cacert.gigi.localisation.Language;
import org.cacert.gigi.output.template.IterableDataset;
import org.cacert.gigi.output.template.Template;
import org.cacert.gigi.pages.account.certs.CertificateRequest;
import org.cacert.gigi.ping.DomainPinger;
import org.cacert.gigi.ping.PingerDaemon;
import org.cacert.gigi.util.AuthorizationContext;
import org.cacert.gigi.util.Notary;

import sun.security.x509.X509Key;

public class Manager extends Page {

    public static final String PATH = "/manager";

    Field f;

    private static HashMap<DomainPingType, DomainPinger> dps;

    private Manager() {
        super("Test Manager");
        try {
            f = EmailAddress.class.getDeclaredField("hash");
            f.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            // TODO
            System.out.println("I don't have 'hash', we are working probably in layered mode. Test Manager may not work.");
            // throw new Error(e);
        }

        try {
            Field gigiInstance = Gigi.class.getDeclaredField("instance");
            gigiInstance.setAccessible(true);
            Gigi g = (Gigi) gigiInstance.get(null);

            Field gigiPinger = Gigi.class.getDeclaredField("pinger");
            gigiPinger.setAccessible(true);
            PingerDaemon pd = (PingerDaemon) gigiPinger.get(g);

            Field f = PingerDaemon.class.getDeclaredField("pingers");
            f.setAccessible(true);
            dps = (HashMap<DomainPingType, DomainPinger>) f.get(pd);
            HashMap<DomainPingType, DomainPinger> pingers = new HashMap<>();
            for (DomainPingType dpt : DomainPingType.values()) {
                pingers.put(dpt, new PingerFetcher(dpt));
            }
            f.set(pd, pingers);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public User[] getAssurers() {
        if (assurers != null) {
            return assurers;
        }
        assurers = new User[10];
        try {
            GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO `notary` SET `from`=?, `to`=?, `points`=?, `location`=?, `date`=?");
            for (int i = 0; i < assurers.length; i++) {
                String mail = "test-assurer" + i + "@example.com";
                User u = User.getByEmail(mail);
                if (u == null) {
                    System.out.println("Creating assurer");
                    createUser(mail);
                    u = User.getByEmail(mail);
                    passCATS(u);
                    ps.setInt(1, u.getId());
                    ps.setInt(2, u.getId());
                    ps.setInt(3, 100);
                    ps.setString(4, "Manager init code");
                    ps.setString(5, "1990-01-01");
                    ps.execute();
                }
                assurers[i] = u;

            }
        } catch (ReflectiveOperationException | GigiApiException e) {
            e.printStackTrace();
        }
        return assurers;
    }

    private void passCATS(User u) {
        GigiPreparedStatement ps = DatabaseConnection.getInstance().prepare("INSERT INTO cats_passed SET user_id=?, variant_id=1");
        ps.setInt(1, u.getId());
        ps.execute();
    }

    private static Manager instance;

    Template t = new Template(Manager.class.getResource("ManagerMails.templ"));

    HashMap<String, LinkedList<String>> emails = new HashMap<>();

    private static TreeSet<String> pingExempt = new TreeSet<>();

    public static Manager getInstance() {
        if (instance == null) {
            instance = new Manager();
        }
        return instance;
    }

    public static class MailFetcher extends EmailProvider {

        public MailFetcher(Properties p) {}

        @Override
        public String checkEmailServer(int forUid, String address) throws IOException {
            return OK;
        }

        @Override
        public synchronized void sendmail(String to, String subject, String message, String from, String replyto, String toname, String fromname, String errorsto, boolean extra) throws IOException {
            HashMap<String, LinkedList<String>> mails = Manager.getInstance().emails;
            LinkedList<String> hismails = mails.get(to);
            if (hismails == null) {
                mails.put(to, hismails = new LinkedList<>());
            }
            hismails.addFirst(subject + "\n" + message);
        }

    }

    public class PingerFetcher extends DomainPinger {

        private DomainPingType dpt;

        public PingerFetcher(DomainPingType dpt) {
            this.dpt = dpt;
        }

        @Override
        public void ping(Domain domain, String configuration, CertificateOwner target, int confId) {
            System.out.println("Test: " + domain);
            if (pingExempt.contains(domain.getSuffix())) {
                enterPingResult(confId, DomainPinger.PING_SUCCEDED, "Succeeded by TestManager pass-by", null);
            } else {
                dps.get(dpt).ping(domain, configuration, target, confId);
            }
        }

    }

    public void batchCreateUsers(String mailPrefix, String domain, int amount, PrintWriter out) {

        try {
            if (amount > 100) {
                out.print("100 at most, please.");
                return;
            }
            for (int i = 0; i < amount; i++) {
                String email = mailPrefix + i + "@" + domain;
                createUser(email);
            }
        } catch (ReflectiveOperationException e) {
            out.println("failed");
            e.printStackTrace();
        } catch (GigiApiException e) {
            out.println("failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createUser(String email) throws GigiApiException, IllegalAccessException {
        Calendar gc = GregorianCalendar.getInstance();
        gc.set(1990, 0, 1);
        User u = new User(email, "xvXV12°§", new Name("Först", "Läst", "Müddle", "Süffix"), new Date(gc.getTime().getTime()), Locale.ENGLISH);
        EmailAddress ea = u.getEmails()[0];
        if (f == null) {
            System.out.println("verification failed");
            return;
        }
        String hash = (String) f.get(ea);

        ea.verify(hash);
    }

    User[] assurers;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getParameter("create") != null) {
            batchCreateUsers(req.getParameter("prefix"), req.getParameter("suffix"), Integer.parseInt(req.getParameter("amount")), resp.getWriter());
            resp.getWriter().println("User batch created.");
        } else if (req.getParameter("addpriv") != null || req.getParameter("delpriv") != null) {
            User u = User.getByEmail(req.getParameter("email"));
            if (u == null) {
                resp.getWriter().println("User not found.");
                return;
            }
            if (req.getParameter("addpriv") != null) {
                u.grantGroup(u, Group.getByString(req.getParameter("priv")));
                resp.getWriter().println("Privilege granted");
            } else {
                u.revokeGroup(u, Group.getByString(req.getParameter("priv")));
                resp.getWriter().println("Privilege revoked");
            }
        } else if (req.getParameter("fetch") != null) {
            String mail = req.getParameter("femail");
            fetchMails(req, resp, mail);
        } else if (req.getParameter("cats") != null) {
            String mail = req.getParameter("catsEmail");
            User byEmail = User.getByEmail(mail);
            if (byEmail == null) {
                resp.getWriter().println("User not found.");
                return;
            }
            passCATS(byEmail);
            resp.getWriter().println("User has been passed CATS");
        } else if (req.getParameter("assure") != null) {
            String mail = req.getParameter("assureEmail");
            User byEmail = User.getByEmail(mail);
            if (byEmail == null) {
                resp.getWriter().println("User not found.");
                return;
            }
            try {
                for (int i = 0; i < getAssurers().length; i++) {
                    Notary.assure(getAssurers()[i], byEmail, byEmail.getName(), byEmail.getDoB(), 10, "Testmanager Assure up code", "2014-11-06");
                }
            } catch (GigiApiException e) {
                throw new Error(e);
            }
            resp.getWriter().println("User has been assured.");
        } else if (req.getParameter("addEmail") != null) {
            User u = User.getByEmail(req.getParameter("addEmailEmail"));
            try {
                EmailAddress ea = new EmailAddress(u, req.getParameter("addEmailNew"), Locale.ENGLISH);
                if (f != null) {
                    String hash = (String) f.get(ea);
                    ea.verify(hash);
                    resp.getWriter().println("Email added and verified");
                } else {
                    resp.getWriter().println("Email added but verificatio failed.");
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                resp.getWriter().println("An internal error occured.");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                resp.getWriter().println("An internal error occured.");
            } catch (GigiApiException e) {
                e.format(resp.getWriter(), Language.getInstance(Locale.ENGLISH));
            }
        } else if (req.getParameter("addCert") != null) {
            User u = User.getByEmail(req.getParameter("addCertEmail"));
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(4096);
                KeyPair kp = kpg.generateKeyPair();
                SPKAC s = new SPKAC((X509Key) kp.getPublic(), "challange");
                Signature sign = Signature.getInstance("SHA512withRSA");
                sign.initSign(kp.getPrivate());

                byte[] res = s.getEncoded(sign);

                CertificateRequest cr = new CertificateRequest(new AuthorizationContext(u, u), Base64.getEncoder().encodeToString(res), "challange");
                cr.update(CertificateRequest.DEFAULT_CN, Digest.SHA512.toString(), "client", null, "", "email:" + u.getEmail(), resp.getWriter(), req);
                Certificate draft = cr.draft();
                draft.issue(null, "2y", u).waitFor(10000);
                if (draft.getStatus() == CertificateStatus.ISSUED) {
                    resp.getWriter().println("added certificate");
                } else {
                    resp.getWriter().println("signer failed");
                }
            } catch (GeneralSecurityException e1) {
                e1.printStackTrace();
                resp.getWriter().println("error");
            } catch (GigiApiException e) {
                e.format(resp.getWriter(), Language.getInstance(Locale.ENGLISH));
            } catch (InterruptedException e) {
                e.printStackTrace();
                resp.getWriter().println("interrupted");
            }

        } else if (req.getParameter("addExDom") != null) {
            String dom = req.getParameter("exemtDom");
            pingExempt.add(dom);
            resp.getWriter().println("Updated domains exempt from pings. Current set: <br/>");
            resp.getWriter().println(pingExempt);
        } else if (req.getParameter("delExDom") != null) {
            String dom = req.getParameter("exemtDom");
            pingExempt.remove(dom);
            resp.getWriter().println("Updated domains exempt from pings. Current set: <br/>");
            resp.getWriter().println(pingExempt);
        }
    }

    private void fetchMails(HttpServletRequest req, HttpServletResponse resp, String mail) throws IOException {
        final LinkedList<String> mails = emails.get(mail);
        HashMap<String, Object> vars = new HashMap<>();
        vars.put("mail", mail);
        if (mails != null) {
            vars.put("mails", new IterableDataset() {

                Iterator<String> s = mails.iterator();

                @Override
                public boolean next(Language l, Map<String, Object> vars) {
                    if ( !s.hasNext()) {
                        return false;
                    }
                    vars.put("body", s.next().replaceAll("(https?://\\S+)", "<a href=\"$1\">$1</a>"));
                    return true;
                }
            });
        }
        t.output(resp.getWriter(), getLanguage(req), vars);
        if (mails == null) {
            resp.getWriter().println("No mails");

        }
    }

    private Template form = new Template(Manager.class.getResource("Manager.templ"));

    @Override
    public boolean needsLogin() {
        return false;
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        getAssurers();
        String pi = req.getPathInfo().substring(PATH.length());
        if (pi.length() > 1 && pi.startsWith("/fetch-")) {
            String mail = pi.substring(pi.indexOf('-', 2) + 1);
            fetchMails(req, resp, mail);
            return;
        }

        form.output(resp.getWriter(), getLanguage(req), new HashMap<String, Object>());
    }
}
